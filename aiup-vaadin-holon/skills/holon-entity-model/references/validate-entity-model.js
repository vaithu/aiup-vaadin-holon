#!/usr/bin/env node
/*
 * Validates docs/entity_model.md against the /entity-model format and the Holon + holon-saas conventions.
 *
 *   node validate-entity-model.js [docs/entity_model.md] [--external APP_USER] [--no-audit-check]
 *
 * Errors make the exit code 1; warnings do not. Plain Node, no dependencies.
 *
 * Format (aiup-core /entity-model): `### UPPER_SNAKE` heading, one description sentence, a table with exactly the
 * columns Attribute | Description | Data Type | Length/Precision | Validation Rules, a closed vocabulary for types and
 * rules, and a Mermaid diagram with relationships only.
 * Stack (holon-stack.md, flyway-migration): no tenant column, an `id` sequence key, the five audit columns on every
 * entity but framework-provided ones, categorical values as lookup entities (no `Values:` rule), no reserved words,
 * no foreign key across the platform and tenant schemas.
 */
"use strict";
const fs = require("fs");

const args = process.argv.slice(2);
const opt = (n, d) => { const i = args.indexOf("--" + n); return i >= 0 && args[i + 1] && !args[i + 1].startsWith("--") ? args[i + 1] : d; };
const file = args.find(a => !a.startsWith("--") && args[args.indexOf(a) - 1] !== "--external" && args[args.indexOf(a) - 1] !== "--model") || "docs/entity_model.md";
const external = new Set(opt("external", "APP_USER").split(",").filter(Boolean));
const auditCheck = !args.includes("--no-audit-check");

const errors = [], warnings = [];
const err = m => errors.push(m), warn = m => warnings.push(m);

const FRAMEWORK_TABLES = new Set(["tenant_users", "audit_entries", "tenant_billing_invoice", "tenant_billing_subscription", "user_invitation",
  "whatsapp_conversation", "whatsapp_message", "tenant_permissions", "tenant_permission_overrides", "tenant_custom_roles", "tenant_setting_override",
  "tenant_plan_setting_default", "tenant_settings", "tenant_feature_flags", "tenant_audit_log", "tenant", "tenant_features", "tenant_attributes"]);
const RESERVED = new Set(("all,and,any,array,as,asc,asymmetric,authorization,between,both,case,cast,check,constraint,cross,current_date,current_time," +
  "current_timestamp,current_user,day,default,desc,distinct,else,end,except,false,fetch,for,foreign,from,full,grant,group,having,hour,in,inner,intersect," +
  "into,is,join,key,leading,left,level,like,limit,localtime,localtimestamp,minute,minus,month,natural,not,null,of,offset,on,or,order,over,partition," +
  "position,primary,qualify,range,right,row,rownum,rows,second,select,session_user,some,start,symmetric,sysdate,systime,systimestamp,table,to,today,top," +
  "trailing,true,union,unique,user,using,value,values,when,where,window,with,year").split(","));
const TYPES = { Long: v => v === "19", String: v => /^\d+$/.test(v), Integer: v => v === "10", Boolean: v => v === "1",
  Date: v => v === "-", DateTime: v => v === "-", Decimal: v => /^\d+,\d+$/.test(v) };
const RULES = [/^Primary Key, Sequence$/, /^Not Null$/, /^Not Null, Unique$/, /^Not Null, Foreign Key \(([A-Z0-9_]+)\.id\)$/, /^Optional$/,
  /^Not Null, Min: -?[\d.]+, Max: -?[\d.]+$/, /^Not Null, Values: .+$/, /^Not Null, Format: Email$/];
const AUDIT = [["created_by", "String", "100", "Not Null"], ["created_date", "DateTime", "-", "Not Null"], ["last_modified_by", "String", "100", "Optional"],
  ["last_modified_date", "DateTime", "-", "Optional"], ["version", "Long", "19", "Not Null"]];
const HEADER = "|Attribute|Description|DataType|Length/Precision|ValidationRules|";

if (!fs.existsSync(file)) { console.error("not found: " + file); process.exit(2); }
const text = fs.readFileSync(file, "utf8").replace(/\r\n/g, "\n");

// ---- diagram ---------------------------------------------------------------------------------
const diagramMatch = text.match(/```mermaid\n([\s\S]*?)```/);
const inDiagram = new Set();
if (!diagramMatch) err("no Mermaid diagram");
else {
  if (!/^\s*erDiagram/m.test(diagramMatch[1])) err("the Mermaid diagram must start with erDiagram");
  for (const l of diagramMatch[1].split("\n")) {
    const t = l.trim();
    if (!t || t === "erDiagram") continue;
    const rel = t.match(/^([A-Z0-9_]+) [|}o]{1,2}--[|{o]{1,2} ([A-Z0-9_]+) : "[^"]+"$/);
    if (rel) { inDiagram.add(rel[1]); inDiagram.add(rel[2]); }
    else if (/^[A-Z0-9_]+$/.test(t)) inDiagram.add(t);
    else if (/\{\s*$/.test(t) || t === "}") err("the diagram must not hold attributes: " + t);
    else err("diagram line is not a relationship: " + t);
  }
}

// ---- sections --------------------------------------------------------------------------------
const body = text.slice(text.search(/^### /m) >= 0 ? text.search(/^### /m) : 0);
const sections = body.split(/^### /m).slice(1).map(c => {
  const L = c.split("\n");
  const name = L[0].trim();
  const first = L.findIndex(l => l.startsWith("| Attribute |"));
  const rows = [];
  if (first >= 0) for (let i = first + 2; i < L.length && L[i].startsWith("|"); i++) rows.push(L[i].split("|").slice(1, -1).map(x => x.trim()));
  const desc = L.slice(1).find(l => l.trim() && !l.startsWith("|") && !l.startsWith("**"));
  return { name, desc, platform: /^\*\*Schema:\*\* platform/m.test(c), header: first >= 0 ? L[first] : null, rows, text: c };
});
const names = sections.map(s => s.name);
const byName = Object.fromEntries(sections.map(s => [s.name, s]));
names.filter((n, i) => names.indexOf(n) !== i).forEach(n => err(`${n}: defined twice`));

for (const s of sections) {
  const ext = external.has(s.name);
  const at = m => err(`${s.name}: ${m}`);
  if (!/^[A-Z][A-Z0-9_]*$/.test(s.name)) at("the heading must be UPPER_SNAKE_CASE");
  if (!s.desc) at("no one-sentence description");
  if (!s.header || s.header.replace(/ /g, "") !== HEADER) { at("the table must have the columns Attribute | Description | Data Type | Length/Precision | Validation Rules"); continue; }
  if (!inDiagram.has(s.name)) at("not in the Mermaid diagram");
  if (!ext && !s.rows.some(r => r[0] === "id" && /^Primary Key, Sequence$/.test(r[4]))) at("no `id` attribute with `Primary Key, Sequence`");
  const table = s.name.toLowerCase();
  if (!ext && FRAMEWORK_TABLES.has(table)) at(`table name "${table}" is reserved by holon-saas (declare it external)`);
  if (RESERVED.has(table)) at(`table name "${table}" is a reserved word`);
  const attrs = new Set();
  for (const r of s.rows) {
    if (r.length !== 5) { at(`row "${r[0]}" does not have 5 columns`); continue; }
    const [a, d, ty, len, rule] = r;
    const w = m => err(`${s.name}.${a}: ${m}`);
    if (attrs.has(a)) w("defined twice"); attrs.add(a);
    if (!/^[a-z][a-z0-9_]*$/.test(a)) w("attribute names are lower_snake_case");
    if (RESERVED.has(a)) w("a reserved word in H2 or PostgreSQL; rename the attribute");
    if (/^tenant_id$|^country(_code)?$/.test(a) && !s.platform && !ext) w("no tenant or country column on a tenant entity (the schema isolates tenants; the country is a tenant setting)");
    if (!TYPES[ty]) w(`unknown data type "${ty}" (use Long, String, Integer, Decimal, Boolean, Date, DateTime)`);
    else if (!TYPES[ty](len)) w(`length/precision "${len}" is not valid for ${ty}`);
    if (/[A-Za-z(]/.test(len) && ty !== "Decimal") w(`length must be the bare value, not "${len}"`);
    const m = RULES.map(x => rule.match(x)).find(Boolean);
    if (!m) w(`validation rule "${rule}" is not in the closed vocabulary`);
    else {
      if (m[1] && !byName[m[1]] && !external.has(m[1])) w(`foreign key target ${m[1]} is not an entity`);
      if (m[1] && byName[m[1]] && !external.has(m[1]) && byName[m[1]].platform !== s.platform) w(`foreign key to ${m[1]} crosses the platform and tenant schemas`);
      if (m[1] && external.has(m[1]) && s.platform) w(`a platform table cannot reference ${m[1]}, which lives in every tenant schema`);
      if (/Values: /.test(rule)) w("a Values rule is a categorical string; model it as a lookup entity with a Long foreign key");
    }
    if (ty === "Decimal" && !["15,2", "15,3", "5,2"].includes(len)) warn(`${s.name}.${a}: precision ${len}; the convention is 15,2 money, 15,3 quantity, 5,2 percent`);
    if (/_id$/.test(a) && a !== "reference_id" && ty === "Long" && rule === "Optional" && !/References [A-Z0-9_]+/.test(d)) warn(`${s.name}.${a}: an optional foreign key must say "References ENTITY" in exactly that form, so the migration generator creates the constraint (a polymorphic reference_id is exempt)`);
  }
  if (auditCheck && !ext) for (const [a, ty, len, rule] of AUDIT) {
    const r = s.rows.find(x => x[0] === a);
    if (!r) at(`missing audit/version column ${a}`);
    else if (r[2] !== ty || r[3] !== len || r[4] !== rule) at(`${a} should be ${ty} ${len} "${rule}"`);
  }
}
for (const n of inDiagram) if (!byName[n]) err(`${n}: in the diagram but has no section`);

// ---- report ------------------------------------------------------------------------------------
console.log(`${sections.length} entities (${sections.filter(s => s.platform).length} platform, ${external.size} external), ${errors.length} error(s), ${warnings.length} warning(s)`);
if (warnings.length) console.log("\nwarnings:\n" + warnings.map(w => "  - " + w).join("\n"));
if (errors.length) { console.error("\nerrors:\n" + errors.map(e => "  - " + e).join("\n")); process.exit(1); }
console.log("\nOK");

#!/usr/bin/env node
/*
 * Generates Flyway migrations from docs/entity_model.md, one file per table, in dependency order.
 *
 *   node generate-migrations.js [--model docs/entity_model.md] [--out src/main/resources/db]
 *        [--tenant-start 2] [--platform-start 1] [--unique unique.json]
 *        [--external APP_USER=tenant_users] [--no-tenant-tables] [--force] [--dry-run]
 *
 * Output (holon-saas, schema per tenant):
 *   <out>/tenant-migration/V002__create_<table>_table.sql   every entity without "**Schema:** platform"
 *   <out>/migration/V001__create_tenant_tables.sql           the tenant tables holon-saas expects the app to create
 *   <out>/migration/V002__create_<table>_table.sql           entities marked "**Schema:** platform"
 *
 * It reads only what the entity model states, in the closed vocabulary of the /entity-model skill:
 * data types Long, String, Integer, Decimal, Boolean, Date, DateTime; validation rules Primary Key,
 * Not Null, Unique, Foreign Key (TABLE.id), Optional, Min/Max, Values, Format: Email.
 *
 * Nothing is written when a problem is found. An existing migration is never overwritten without --force.
 * No dependencies: plain Node.
 */
"use strict";
const fs = require("fs");
const path = require("path");

// ---- arguments ---------------------------------------------------------------------------------
const args = process.argv.slice(2);
const opt = (name, def) => { const i = args.indexOf("--" + name); return i >= 0 && args[i + 1] && !args[i + 1].startsWith("--") ? args[i + 1] : def; };
const flag = name => args.includes("--" + name);
const modelPath = opt("model", path.join("docs", "entity_model.md"));
const outDir = opt("out", path.join("src", "main", "resources", "db"));
const tenantStart = parseInt(opt("tenant-start", "2"), 10);
const platformStart = parseInt(opt("platform-start", "1"), 10);
const uniquePath = opt("unique", null);
const externalArg = opt("external", "APP_USER=tenant_users");
const withTenantTables = !flag("no-tenant-tables");
const force = flag("force"), dryRun = flag("dry-run");

const external = Object.fromEntries(externalArg.split(",").filter(Boolean).map(p => p.split("=")));
// unique.json:  { "ENTITY": ["col1", "col2"] }      composite UNIQUE constraint
const composite = uniquePath ? JSON.parse(fs.readFileSync(uniquePath, "utf8")) : {};

// ---- reserved names ----------------------------------------------------------------------------
// holon-saas FrameworkReservedTableNames: tables that already exist in every tenant schema.
const FRAMEWORK_TABLES = new Set(["tenant_users", "audit_entries", "tenant_billing_invoice", "tenant_billing_subscription",
  "user_invitation", "whatsapp_conversation", "whatsapp_message", "tenant_permissions", "tenant_permission_overrides",
  "tenant_custom_roles", "tenant_setting_override", "tenant_plan_setting_default", "tenant_settings",
  "tenant_feature_flags", "tenant_audit_log", "tenant", "tenant_features", "tenant_attributes"]);
// Words that are reserved, or risky, as a bare identifier in H2 or PostgreSQL.
const RESERVED = new Set(("all,and,any,array,as,asc,asymmetric,authorization,between,both,case,cast,check,constraint,cross,current_date," +
  "current_time,current_timestamp,current_user,day,default,desc,distinct,else,end,except,false,fetch,for,foreign,from,full,grant,group," +
  "having,hour,in,inner,intersect,into,is,join,key,leading,left,level,like,limit,localtime,localtimestamp,minute,minus,month,natural," +
  "not,null,of,offset,on,or,order,over,partition,position,primary,qualify,range,right,row,rownum,rows,second,select,session_user," +
  "some,start,symmetric,sysdate,systime,systimestamp,table,to,today,top,trailing,true,union,unique,user,using,value,values,when," +
  "where,window,with,year").split(","));

// ---- parse the entity model --------------------------------------------------------------------
const model = fs.readFileSync(modelPath, "utf8").replace(/\r\n/g, "\n");
const sections = model.split(/^### /m).slice(1).map(c => {
  const L = c.split("\n");
  const name = L[0].trim();
  const desc = L.slice(1).find(l => l.trim() && !l.startsWith("|") && !l.startsWith("**")) || "";
  const platform = /^\*\*Schema:\*\* platform/m.test(c);
  const first = L.findIndex(l => l.startsWith("| Attribute |"));
  const rows = [];
  if (first >= 0) for (let i = first + 2; i < L.length && L[i].startsWith("|"); i++) rows.push(L[i].split("|").slice(1, -1).map(x => x.trim()));
  return { name, desc, platform, rows };
});
const byName = Object.fromEntries(sections.map(s => [s.name, s]));
const tableOf = n => external[n] || n.toLowerCase();
const problems = [];
const problem = m => problems.push(m);

const TYPE = { Long: () => "BIGINT", String: l => `VARCHAR(${l})`, Integer: () => "INTEGER", Decimal: l => `DECIMAL(${l})`,
  Boolean: () => "BOOLEAN", Date: () => "DATE", DateTime: () => "TIMESTAMP WITH TIME ZONE" };

// ---- build one table ---------------------------------------------------------------------------
function build(sec) {
  const t = tableOf(sec.name);
  const cols = [], tail = [], fkIndexes = [], deps = new Set();
  if (FRAMEWORK_TABLES.has(t)) problem(`${sec.name}: table name "${t}" is reserved by holon-saas (declare it with --external)`);
  if (RESERVED.has(t)) problem(`${sec.name}: table name "${t}" is a reserved word`);
  for (const [a, d, ty, len, rule] of sec.rows) {
    if (!TYPE[ty]) { problem(`${sec.name}.${a}: unknown data type "${ty}"`); continue; }
    if (RESERVED.has(a)) problem(`${sec.name}.${a}: column name is a reserved word in H2 or PostgreSQL; rename the attribute in the entity model`);
    if (/^Primary Key/.test(rule)) { cols.push(`${a.padEnd(20)} BIGINT DEFAULT nextval('${t}_seq') PRIMARY KEY`); continue; }
    let def = `${a.padEnd(20)} ${TYPE[ty](len)}`;
    // audit and version columns follow the flyway-migration skill
    if (a === "version") def += " NOT NULL DEFAULT 0";
    else if (a === "created_by") def += " NOT NULL DEFAULT 'system'";
    else if (a === "created_date") def += " NOT NULL DEFAULT CURRENT_TIMESTAMP";
    else { if (/^Not Null/.test(rule)) def += " NOT NULL"; if (/, Unique/.test(rule)) def += " UNIQUE"; }
    let target = null;
    const fk = rule.match(/Foreign Key \((\w+)\.id\)/);
    if (fk) target = fk[1];
    else if (rule === "Optional" && ty === "Long" && /_id$/.test(a)) { const m = d.match(/References ([A-Z_]+)/); if (m && byName[m[1]]) target = m[1]; }
    if (target) {
      if (!byName[target]) problem(`${sec.name}.${a}: foreign key target ${target} is not an entity`);
      else {
        const tgt = byName[target];
        if (!external[target] && tgt.platform !== sec.platform) problem(`${sec.name}.${a}: foreign key crosses schemas (${sec.platform ? "platform" : "tenant"} -> ${tgt.platform ? "platform" : "tenant"})`);
        if (external[target] && sec.platform) problem(`${sec.name}.${a}: a platform table cannot reference ${external[target]}, which lives in every tenant schema`);
        def += ` REFERENCES ${tableOf(target)}(id)`;
        if (target !== sec.name && !external[target]) deps.add(target);
        fkIndexes.push(a);
      }
    }
    const mm = rule.match(/Min: ([-\d.]+), Max: ([-\d.]+)/);
    if (mm) tail.push(`CONSTRAINT ck_${t}_${a} CHECK (${a} >= ${mm[1]} AND ${a} <= ${mm[2]})`);
    if (/Values: /.test(rule)) problem(`${sec.name}.${a}: a Values rule is a categorical string; model it as a lookup entity`);
    cols.push(def);
  }
  const uq = composite[sec.name];
  if (uq) {
    for (const c of uq) if (!sec.rows.some(r => r[0] === c)) problem(`${sec.name}: composite unique column "${c}" is not an attribute`);
    tail.push(`CONSTRAINT uq_${t}_${uq.join("_")} UNIQUE (${uq.join(", ")})`);
  }
  for (const n of [...tail.map(c => c.match(/CONSTRAINT (\w+)/)[1]), ...fkIndexes.map(c => `idx_${t}_${c}`)])
    if (n.length > 63) problem(`${sec.name}: identifier "${n}" is longer than 63 characters (PostgreSQL limit)`);
  const body = [...cols, ...tail].map(x => "    " + x).join(",\n");
  const sql = `-- ${sec.name}: ${sec.desc}\nCREATE SEQUENCE ${t}_seq START WITH 1 INCREMENT BY 50;\n\nCREATE TABLE ${t}\n(\n${body}\n);\n` +
    (fkIndexes.length ? "\n" + fkIndexes.map(c => `CREATE INDEX idx_${t}_${c} ON ${t} (${c});`).join("\n") + "\n" : "");
  return { sec, t, sql, deps };
}

const built = sections.filter(s => !external[s.name] && s.rows.length).map(build);
const order = (group) => {
  const out = [], done = new Set();
  let rest = [...group];
  while (rest.length) {
    const ready = rest.filter(b => [...b.deps].every(d => done.has(d)));
    if (!ready.length) { problem("dependency cycle among: " + rest.map(b => b.sec.name).join(", ")); break; }
    for (const b of ready) { out.push(b); done.add(b.sec.name); }
    rest = rest.filter(b => !ready.includes(b));
  }
  return out;
};
const tenantTables = order(built.filter(b => !b.sec.platform));
const platformTables = order(built.filter(b => b.sec.platform));

// ---- platform tables holon-saas expects the application to create ------------------------------
// Mirrors com.holonplatform.multitenant.TenantDetails. Re-read that entity for the holon-saas version in use.
const TENANT_TABLES = `-- Platform schema tables that holon-saas expects the application to create.
-- They mirror com.holonplatform.multitenant.TenantDetails exactly (table tenant,
-- element collections tenant_features and tenant_attributes).

CREATE TABLE tenant
(
    tenant_id VARCHAR(64)  NOT NULL PRIMARY KEY,
    name      VARCHAR(255),
    plan      VARCHAR(32)  NOT NULL,
    status    VARCHAR(32)  NOT NULL,
    locale    VARCHAR(32),
    timezone  VARCHAR(64),
    theme     VARCHAR(64)
);

CREATE INDEX idx_tenant_status ON tenant (status);
CREATE INDEX idx_tenant_plan ON tenant (plan);

CREATE TABLE tenant_features
(
    tenant_id   VARCHAR(64)  NOT NULL REFERENCES tenant (tenant_id),
    feature_key VARCHAR(100) NOT NULL,
    PRIMARY KEY (tenant_id, feature_key)
);

CREATE TABLE tenant_attributes
(
    tenant_id  VARCHAR(64)  NOT NULL REFERENCES tenant (tenant_id),
    attr_key   VARCHAR(255) NOT NULL,
    attr_value TEXT,
    PRIMARY KEY (tenant_id, attr_key)
);
`;

// ---- write ---------------------------------------------------------------------------------------
const tenantDir = path.join(outDir, "tenant-migration"), platformDir = path.join(outDir, "migration");
const num = n => String(n).padStart(3, "0");
const plan = [];
tenantTables.forEach((b, i) => plan.push({ dir: tenantDir, file: `V${num(tenantStart + i)}__create_${b.t}_table.sql`, sql: b.sql }));
let pv = platformStart;
if (withTenantTables) plan.push({ dir: platformDir, file: `V${num(pv++)}__create_tenant_tables.sql`, sql: TENANT_TABLES });
platformTables.forEach(b => plan.push({ dir: platformDir, file: `V${num(pv++)}__create_${b.t}_table.sql`, sql: b.sql }));

if (!force) for (const dir of [tenantDir, platformDir]) {
  if (fs.existsSync(dir) && fs.readdirSync(dir).some(f => /^V\d+__create_.*\.sql$/.test(f)))
    problem(`${dir} already contains generated migrations; use --force to overwrite them (they may have been edited by hand)`);
}

console.log(`entities: ${sections.length}; tenant tables: ${tenantTables.length}; platform tables: ${platformTables.length}` +
  `${withTenantTables ? " (+ tenant, tenant_features, tenant_attributes)" : ""}; external: ${Object.keys(external).join(", ") || "none"}`);
if (problems.length) {
  console.error(`\n${problems.length} problem(s); nothing was written:\n` + problems.map(p => "  - " + p).join("\n"));
  process.exit(1);
}
if (dryRun) { console.log("dry run: " + plan.length + " files would be written"); process.exit(0); }
for (const p of plan) { fs.mkdirSync(p.dir, { recursive: true }); fs.writeFileSync(path.join(p.dir, p.file), p.sql); }
console.log(`wrote ${plan.length} files: tenant-migration ${tenantTables.length} (V${num(tenantStart)}..V${num(tenantStart + tenantTables.length - 1)}), migration ${plan.length - tenantTables.length}`);

# Country Variation

Applies to SaaS applications whose behaviour depends on the country a tenant operates in:
customer registration details, tax, payment methods, and presentation formats (currency,
date, number, address).

## Where each fact lives

| Fact | Artifact | How |
|---|---|---|
| Supported countries and why | `docs/vision.md` | Business Constraints and Out of Scope |
| Country-level requirements | `docs/requirements.md` | FR for setting the country; C-XXX per country obligation (e.g. GST invoicing, state sales tax) |
| Country-dependent fields, formats and validation | `docs/entity_model.md` | Stated once per entity, per country (e.g. Tax ID type and format, address format) |
| Country-dependent policy | `docs/business_rules.md` | `GR-NNN` entries in a **Country rules** section, grouped by country; each starts with `Applies to: <country>` |
| Country behaviour inside a flow | `docs/use_cases/UC-XXX-*.md` | A local `BR-NNN` that `Realizes` the `GR-NNN`, plus an alternative flow only where the steps genuinely differ |
| Country journeys | `docs/test_cases/TC-XXX-*.md` | One journey per country with literal data (e.g. an Indian B2B customer invoiced with GST) |

No separate `country_rules.md`: country policy reuses the `GR-NNN` catalogue, so the
existing traceability sensors already verify that every rule is cited and every citation resolves.

## Rules for specs

1. **One use case per goal, not per country.** Never write `UC-001-register-customer-india.md`.
   Vary through business rules and alternative flows. Split a use case only when a country's
   flow has its own actor, preconditions and rules.
2. **The country is a precondition, not an in-flow check.** The tenant's operating country is
   set at tenant creation (see `holon-stack.md`, Multi-Tenancy, rule 5). Steps say "per the
   tenant's country", never "if India".
3. **Validate by rule.** "System validates that the Tax ID is valid for the tenant's country
   (BR-004)", where BR-004 realizes the country `GR`s.
4. **Zone 1 only.** State observable facts (required fields, tax rate, message content). Never
   state how rules are configured, stored or looked up.
5. **Tax is reproducible.** A sales order keeps the tax it was created with; later rule changes
   do not alter past orders. State this as a postcondition of the sales-order use cases.
6. **Customer location versus tenant country.** Tenant country decides compliance. Where the
   customer's location changes the tax (e.g. export), that is a business rule in the sales-order
   use case, not a second country setting.

## Code

Country-dependent selectable values (country, currency, tax category, payment method) are
lookup entities loaded from Flyway-managed tables, never Java enums (see `holon-stack.md`,
Form authoring rule 3). The tenant country is read through the `holon-saas` tenant settings
service.

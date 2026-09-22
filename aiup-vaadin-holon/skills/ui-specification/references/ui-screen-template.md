# UI Screen Specification Template

Copy this template to `docs/ui/screens/<screen>.md` and replace the placeholders.

```markdown
# UI-XXX — <Screen Name>

## Purpose

<What the user accomplishes here.>

## Related Requirements

- FR-XXX
- NFR-XXX

## Related Use Cases

- UC-XXX

## Actors / Roles

| Role | Access | Actions |
|------|--------|---------|
| ...  | ...    | ...     |

## Layout

### Region A

...

### Region B

...

## Components

| Region | Component | Content | Interaction |
|--------|-----------|---------|-------------|
| ...    | ...       | ...     | ...         |

## User Interactions

1. ...
2. ...

## States

### Empty

...

### Loading

...

### Normal

...

### Validation Error

...

### Permission Restricted

...

### Business-Rule Error

...

### Success

...

## Responsive Behavior

### Desktop

...

### Tablet

...

### Mobile

...

## Accessibility

...

## Visual Reference

`docs/mockups/<screen>.html`

## Implementation Notes

<Presentation/component guidance only. Business rules belong in requirements and
use-case specifications.>
```

# Form Contract Checklist

- [ ] Correct path chosen (AiCrudPage vs Provider vs lowcode designer Skill).
- [ ] Dict fields use useDict/DictSelect/DictTag.
- [ ] Field types match controls (user/dept/number/switch); no silent input fallback.
- [ ] Generated CRUD api-config uses POST getById/edit/remove and `:id` placeholders.
- [ ] Schemas depending on async dicts are `computed`.
- [ ] Task Provider only saves writable fields; node permissions respected.
- [ ] Long IDs stringified in selects; file fields store ids.
- [ ] Custom slots update form through official update channel.

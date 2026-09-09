# نقشهٔ فایل‌های کلیدی

```
hesabino/
├── README.md
├── docs/
│   ├── 00-PRODUCT-DECISIONS.md
│   ├── 01-UX-DESIGN.md          وایرفریم + توکن + navigation
│   ├── 02-ARCHITECTURE.md       اسکیما + SMS flow + dedup
│   ├── 03-ACCEPTANCE.md
│   └── icon-preview.png
├── prototype/hesabino.html      پیش‌نمایش تعاملی UI
└── app/src/main/java/ir/hesabino/app/
    ├── engine/normalize/        NumberNormalizer, AmountParser
    ├── engine/parser/           Generic + بانک‌های ایران + Registry
    ├── engine/fingerprint/      Dedup پایدار
    ├── engine/rules/            RuleEngine + dry-run
    ├── sms/                     SmsReceiver, SmsImportWorker, SmsIngestor
    ├── data/                    Room v1 + DataStore + Backup JSON/CSV
    └── ui/                      ۴ تب + AddTx + RuleBuilder + Lock
```

تست‌ها: `app/src/test/java/ir/hesabino/app/`
- normalize numbers
- parse amount
- rule matching
- fingerprint / re-import
- generic + mellat parser
- jalali 2026-09-09 = ۱۸ شهریور ۱۴۰۵

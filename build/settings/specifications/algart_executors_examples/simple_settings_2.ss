{
  "app": "settings",
  "name": "Simple_settings_2",
  "category": "$no-prefix$~examples.settings",
  "id": "0b567f0f-f306-47e2-95ce-fac597510dcd",
  "controls": [
    {
      "name": "str",
      "value_type": "String",
      "description": "Example of string enum parameter",
      "edition_type": "enum",
      "items": [
        {
          "value": "VARIANT_A",
          "caption": "Variant A"
        },
        {
          "value": "VARIANT_B",
          "caption": "Variant B"
        }
      ],
      "default": "VARIANT_A"
    },
    {
      "name": "k",
      "caption": "k (int)",
      "description": "Example of integer value",
      "value_type": "int",
      "edition_type": "value",
      "default": 12
    },
    {
      "name": "check",
      "description": "Example of boolean value",
      "value_type": "boolean",
      "edition_type": "value",
      "default": false
    }
  ]
}



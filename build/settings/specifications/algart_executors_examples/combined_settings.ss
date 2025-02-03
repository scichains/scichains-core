{
  "app": "main-settings",
  "name": "Simple combined settings",
  "category": "$no-prefix$~examples.settings",
  "id": "0526290c-5160-4509-82d7-9687e594ab53",
  "controls": [
    {
      "name": "task_name",
      "caption": "Task name",
      "value_type": "String",
      "edition_type": "value",
      "default": "My first work"
    },
    {
      "name": "Simple_settings_1",
      "caption": "Simple settings (1)",
      "value_class_name": "SimpleSettings1",
      "_rem": "(Allows smart search by class name: better than checking the name)",
      "description": "This is sub-settings: JSON, build according to \"simple_settings_1.ss\"",
      "value_type": "settings",
      "edition_type": "value",
      "default": {}
    },
    {
      "name": "Simple_settings_2",
      "caption": "Simple settings (2)",
      "description": "This is sub-settings: JSON, build according to \"simple_settings_2.ss\"",
      "_rem": "(Can be smart found by \"name\" only)",
      "value_type": "settings",
      "edition_type": "value",
      "default": {}
    },
    {
      "name": "Simple_settings_3",
      "caption": "Simple settings (3)",
      "description": "This is sub-settings: JSON, build according to \"simple_settings_3.ss\"",
      "settings_id": "21fa3eec-d305-4820-8a7a-636c7ed58248",
      "_rem": "(Explicit specifying settings ID: smart search is not necessary)",
      "value_type": "settings",
      "edition_type": "value",
      "default": {}
    }
  ]
}



# ConvertApp

## API Key Setup

This application uses external APIs that require authentication. For security reasons, API keys are not included in the repository.

To set up the API keys:

1. Locate the file `app/src/main/java/com/nlu/convertapp/api/ApiKeys.template.java`
2. Create a copy of this file named `ApiKeys.java` in the same directory
3. Replace the placeholder values with your actual API keys:
   - ElevenLabs API keys
   - Viettel AI API keys

**Note:** The `ApiKeys.java` file is excluded from git tracking in `.gitignore` to prevent accidentally committing your private API keys. 
# Delete old Snyk files

This small program deletes old Snyk files - specifically, `.github/workflows/snyk.yml`

## Creating the GitHub App

### GitHub App for a single user account

GitHub defines a [url-format](https://docs.github.com/en/apps/sharing-github-apps/registering-a-github-app-using-url-parameters) for helping create GitHub Apps wih specified permissions.
You can just click this link to get taken to a pre-filled page to create the new GitHub App - you'll just need to customise the app name:

https://github.com/settings/apps/new?name=delete-old-snyk-files&url=https://github.com/rtyley/delete-old-snyk-files&public=false&workflows=write&single_file=write&single_file_name=.github/workflows/snyk.yml&webhook_active=false

### GitHub App for an organisation account

You can use the link above, but change the url so that it starts like this (the url query parameters stay the same),
and replace `ORGANIZATION` with your organisation's name (eg `guardian`):

```
github.com/organizations/ORGANIZATION/settings/apps/new
```

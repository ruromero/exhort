# SPDX Java Library – Parsing API (without categories)

The **spdx-java-library** (already on your classpath via `spdx-jackson-store`) provides a parsing API that is more capable than manual string splitting for SPDX license expressions. It handles both **deprecated** and **modern** forms and gives you structured metadata (FSF, OSI, deprecated, exceptions, AND/OR/WITH).

## What you get (vs current approach)

| Need | Your current approach | spdx-java-library |
|------|------------------------|-------------------|
| Deprecated IDs | Manual `-with-` / `-only` parsing, own `licenses.json` | Parses `GPL-2.0-with-classpath-exception` as a single **ListedLicense**; ID and metadata from SPDX list |
| Modern form | No ` WITH ` handling in parser | Parses `GPL-2.0-only WITH Classpath-exception-2.0` as **WithAdditionOperator** (license + exception) |
| OSI / FSF / deprecated | From your `licenses.json` (filtered) | **ListedLicense**: `getIsOsiApproved()`, `getIsFsfLibre()`, `getIsDeprecatedLicenseId()` (and `getDeprecatedVersion()`) |
| Exception | Manual suffix / `exceptions.json` | **WithAdditionOperator**: `getSubjectAddition()` → **ListedLicenseException** (id, name, etc.) |
| AND / OR structure | `split("( AND \| OR )")` | **ConjunctiveLicenseSet** (AND), **DisjunctiveLicenseSet** (OR); walk tree for each member |

So **yes**, the library can give you a parsing API that is better than the current one for: deprecated vs modern IDs, FSF/OSI/deprecated, which exception, and whether the expression is AND vs OR (and nested).

## Initialization

Use the library’s default store (and optional config) before parsing:

```java
SpdxModelFactory.init();
```

Optional: to avoid network and use only JAR-bundled license list:

- `-Dorg.spdx.useJARLicenseInfoOnly=true`  
or set in `/resources/spdx-java-library.properties`.

## Parsing entry points

1. **LicenseExpressionParser** (recommended for expressions with AND/OR/WITH):

   ```java
   IModelStore store = new InMemSpdxStore(); // or DefaultModelStore
   AnyLicenseInfo root = LicenseExpressionParser.parseLicenseExpression(
       expression, store, null, null, null, null);
   ```

2. **LicenseInfoFactory** (simpler when you only need a single expression parsed with default store):

   ```java
   AnyLicenseInfo root = LicenseInfoFactory.parseSPDXLicenseString(expression);
   ```

Both accept:

- **Deprecated**: `"GPL-2.0-with-classpath-exception"`  
- **Modern**: `"GPL-2.0-only WITH Classpath-exception-2.0"`  
- **Multi**: `"MIT AND GPL-2.0-only"`, `"MIT OR Apache-2.0"`, `"(A AND B) OR C"`

## Result type (AST)

`AnyLicenseInfo` is the root; you need to branch on concrete type and recurse:

| Type | Meaning | What to use |
|------|--------|-------------|
| **ListedLicense** | Single listed license (including deprecated single-id like `GPL-2.0-with-classpath-exception`) | `getId()`, `getIsOsiApproved()`, `getIsFsfLibre()`, `getIsDeprecatedLicenseId()`, `getDeprecatedVersion()`, `getName()`, etc. |
| **WithAdditionOperator** | License WITH exception (modern form) | `getSubjectExtendableLicense()` → license (e.g. ListedLicense); `getSubjectAddition()` → exception (e.g. ListedLicenseException: id, name) |
| **ConjunctiveLicenseSet** | AND | Members via model (e.g. get collection of members); recurse on each for AND semantics |
| **DisjunctiveLicenseSet** | OR | Same; recurse on each for OR semantics |
| **OrLaterOperator** | `+` (e.g. `GPL-2.0+`) | Subject license + “or later” semantics |

So: **AND/OR** → ConjunctiveLicenseSet / DisjunctiveLicenseSet; **WITH** → WithAdditionOperator; **single or deprecated compound ID** → ListedLicense.

## Helpers (no parsing)

- **LicenseInfoFactory.getListedLicenseById(String licenseId)**  
  Returns **ListedLicense** or null. Use for resolving an ID (including deprecated) to get OSI/FSF/deprecated without parsing an expression.
- **LicenseInfoFactory.listedLicenseIdCaseSensitive(String)**  
  Normalize case for an ID.
- **LicenseInfoFactory.isSpdxListedLicenseId(String)**, **isSpdxListedExceptionId(String)**  
  Check if ID is in the SPDX list.

## Minimal example (no categories)

```java
SpdxModelFactory.init();
IModelStore store = new InMemSpdxStore();

// Deprecated form
AnyLicenseInfo a = LicenseExpressionParser.parseLicenseExpression(
    "GPL-2.0-with-classpath-exception", store, null, null, null, null);
if (a instanceof ListedLicense) {
  ListedLicense lic = (ListedLicense) a;
  String id = lic.getId();  // or from objectUri/toString
  Boolean osi = lic.getIsOsiApproved();
  Boolean fsf = lic.getIsFsfLibre();
  Boolean deprecated = lic.getIsDeprecatedLicenseId();
  // no separate “exception” object for deprecated form; it’s one listed license
}

// Modern form
AnyLicenseInfo b = LicenseExpressionParser.parseLicenseExpression(
    "GPL-2.0-only WITH Classpath-exception-2.0", store, null, null, null, null);
if (b instanceof WithAdditionOperator) {
  WithAdditionOperator with = (WithAdditionOperator) b;
  ExtendableLicense subject = with.getSubjectExtendableLicense();  // e.g. ListedLicense
  LicenseAddition addition = with.getSubjectAddition();             // e.g. ListedLicenseException
  // subject: OSI/FSF/deprecated via getIsOsiApproved(), getIsFsfLibre(), getIsDeprecatedLicenseId()
  // addition: exception id/name (e.g. ListedLicenseException.getId(), getName())
}

// Multi-license
AnyLicenseInfo c = LicenseExpressionParser.parseLicenseExpression(
    "MIT OR (GPL-2.0-only AND BSD-3-Clause)", store, null, null, null, null);
// c is DisjunctiveLicenseSet (OR); members: MIT, ConjunctiveLicenseSet(AND); recurse to get AND/OR and each license.
```

You’d implement a small tree walk (instanceof ConjunctiveLicenseSet / DisjunctiveLicenseSet / WithAdditionOperator / ListedLicense / OrLaterOperator) to collect all licenses, exceptions, and AND vs OR.

## Summary

- **Deprecated vs modern**: Both `GPL-2.0-with-classpath-exception` and `GPL-2.0-only WITH Classpath-exception-2.0` are supported; first as a single **ListedLicense**, second as **WithAdditionOperator**.
- **FSF / OSI / deprecated**: From **ListedLicense** (and from the license inside **WithAdditionOperator**).
- **Exception**: From **WithAdditionOperator.getSubjectAddition()** (e.g. **ListedLicenseException**).
- **AND / OR**: From **ConjunctiveLicenseSet** and **DisjunctiveLicenseSet**; structure is in the tree, not in a single string split.

So the spdx-java-library can replace your custom parsing for “parse deprecated + modern, get FSF/OSI/deprecated, exception, and AND/OR” without considering categories; categories can still be applied by you on top of this AST (e.g. from your `license-categories.yaml` by license ID and exception suffix).

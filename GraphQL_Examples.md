# GraphQL Examples:

https://docs.github.com/en/graphql/overview/explorer

## Rate Limit

Each SecurityVulnerability query only consumes 1 point out of the 5000 per hour

```
  rateLimit(dryRun: true) {
    remaining
    used
    nodeCount
  }
```

## Fragments

These are to simplify queries

```graphql
fragment VulnData on SecurityVulnerability {
  advisory {
    cvss {
      vectorString
      score
    }
    summary
    identifiers {
      type
      value
    }
  }
  package {
    name
    ecosystem
  }
  severity
  vulnerableVersionRange
}

fragment PageData on PageInfo {
  hasNextPage
  hasPreviousPage
  endCursor
  startCursor
}
```

## Multiple Queries

```
{
  test1: securityVulnerabilities(
    first: 100
    ecosystem: MAVEN
    package: "org.postgresql:postgresql"
  ) {
    pageInfo {
      ...PageData
    }
    totalCount
    nodes {
      ...VulnData
    }
  }
  test2: securityVulnerabilities(
    first: 100
    ecosystem: MAVEN
    package: "com.fasterxml.jackson.core:jackson-databind"
  ) {
    pageInfo {
      ...PageData
    }
    totalCount
    nodes {
      ...VulnData
    }
  }
}
```

## Parameterized Query

Query:

```
query getVulns($ecosystem:SecurityAdvisoryEcosystem!, $package:String!) {
  securityVulnerabilities(
    first: 100
    ecosystem: $ecosystem
    package: $package
  ) {
    pageInfo {
      ...PageData
    }
    totalCount
    nodes {
      ...VulnData
    }
  }
}
```

Variables:

```json
{
  "ecosystem": "MAVEN",
  "package": "com.fasterxml.jackson.core:jackson-databind"
}
```
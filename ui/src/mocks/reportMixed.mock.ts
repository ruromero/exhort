import { AppData } from '@app/api/report';

export const reportMixed: AppData = {
  packagePath: 'https://central.sonatype.com/artifact/',
  remediationPath: 'https://maven.repository.redhat.com/ga/',
  providerPrivateData: null,
  vexPath: 'https://tc-storage-mvp.s3.amazonaws.com/vexes/',
  report: {
    "summary": {
      "dependencies": {
        "total": 9,
        "direct": 2,
        "transitive": 7
      },
      "providers": {
        "oss-index": {
          "status": {
            "ok": true,
            "name": "oss-index",
            "code": 200,
            "message": "OK"
          },
          "sources": {
            "oss-index": {
              "direct": 0,
              "transitive": 3,
              "total": 3,
              "dependencies": 2,
              "critical": 0,
              "high": 3,
              "medium": 0,
              "low": 0,
              "remediations": 0,
              "recommendations": 0
            }
          }
        },
        "snyk": {
          "status": {
            "ok": true,
            "name": "snyk",
            "code": 200,
            "message": "OK"
          },
          "sources": {
            "snyk": {
              "direct": 0,
              "transitive": 4,
              "total": 4,
              "dependencies": 2,
              "critical": 0,
              "high": 1,
              "medium": 3,
              "low": 0,
              "remediations": 0,
              "recommendations": 0
            }
          }
        }
      }
    },
    "dependencies": [
      {
        "ref": "pkg:maven/io.quarkus/quarkus-hibernate-orm@2.13.5.Final",
        "transitive": [
          {
            "ref": "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.1",
            "issues": [
              {
                "id": "SNYK-JAVA-COMFASTERXMLJACKSONCORE-2421244",
                "title": "Denial of Service (DoS)",
                "source": {
                  "provider": "snyk",
                  "origin": "snyk"
                },
                "cvss": {
                  "attackVector": "Network",
                  "attackComplexity": "Low",
                  "privilegesRequired": "None",
                  "userInteraction": "None",
                  "scope": "Unchanged",
                  "confidentialityImpact": "None",
                  "integrityImpact": "None",
                  "availabilityImpact": "High",
                  "cvss": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
                },
                "cvssScore": 7.5,
                "severity": "HIGH",
                "cves": [
                  "CVE-2020-36518"
                ],
                "unique": false,
                "remediation": {
                  "fixedIn": [
                    "2.12.6.1",
                    "2.13.2.1",
                    "2.14.0"
                  ]
                }
              },
              {
                "id": "SNYK-JAVA-COMFASTERXMLJACKSONCORE-3038424",
                "title": "Denial of Service (DoS)",
                "source": {
                  "provider": "snyk",
                  "origin": "snyk"
                },
                "cvss": {
                  "attackVector": "Network",
                  "attackComplexity": "High",
                  "privilegesRequired": "None",
                  "userInteraction": "None",
                  "scope": "Unchanged",
                  "confidentialityImpact": "None",
                  "integrityImpact": "None",
                  "availabilityImpact": "High",
                  "exploitCodeMaturity": "Proof of concept code",
                  "cvss": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H/E:P"
                },
                "cvssScore": 5.9,
                "severity": "MEDIUM",
                "unique": true,
                "remediation": {
                  "fixedIn": [
                    "2.13.4"
                  ]
                }
              },
              {
                "id": "SNYK-JAVA-COMFASTERXMLJACKSONCORE-3038426",
                "title": "Denial of Service (DoS)",
                "source": {
                  "provider": "snyk",
                  "origin": "snyk"
                },
                "cvss": {
                  "attackVector": "Network",
                  "attackComplexity": "High",
                  "privilegesRequired": "None",
                  "userInteraction": "None",
                  "scope": "Unchanged",
                  "confidentialityImpact": "None",
                  "integrityImpact": "None",
                  "availabilityImpact": "High",
                  "exploitCodeMaturity": "Proof of concept code",
                  "cvss": "CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:N/I:N/A:H/E:P"
                },
                "cvssScore": 5.9,
                "severity": "MEDIUM",
                "cves": [
                  "CVE-2022-42003"
                ],
                "unique": false,
                "remediation": {
                  "fixedIn": [
                    "2.12.7.1",
                    "2.13.4.2"
                  ]
                }
              }
            ],
            "highestVulnerability": {
              "id": "SNYK-JAVA-COMFASTERXMLJACKSONCORE-2421244",
              "title": "Denial of Service (DoS)",
              "source": {
                "provider": "snyk",
                "origin": "snyk"
              },
              "cvss": {
                "attackVector": "Network",
                "attackComplexity": "Low",
                "privilegesRequired": "None",
                "userInteraction": "None",
                "scope": "Unchanged",
                "confidentialityImpact": "None",
                "integrityImpact": "None",
                "availabilityImpact": "High",
                "cvss": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
              },
              "cvssScore": 7.5,
              "severity": "HIGH",
              "cves": [
                "CVE-2020-36518"
              ],
              "unique": false,
              "remediation": {
                "fixedIn": [
                  "2.12.6.1",
                  "2.13.2.1",
                  "2.14.0"
                ]
              }
            }
          }
        ],
        "highestVulnerability": {
          "id": "SNYK-JAVA-COMFASTERXMLJACKSONCORE-2421244",
          "title": "Denial of Service (DoS)",
          "source": {
            "provider": "snyk",
            "origin": "snyk"
          },
          "cvss": {
            "attackVector": "Network",
            "attackComplexity": "Low",
            "privilegesRequired": "None",
            "userInteraction": "None",
            "scope": "Unchanged",
            "confidentialityImpact": "None",
            "integrityImpact": "None",
            "availabilityImpact": "High",
            "cvss": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
          },
          "cvssScore": 7.5,
          "severity": "HIGH",
          "cves": [
            "CVE-2020-36518"
          ],
          "unique": false,
          "remediation": {
            "fixedIn": [
              "2.12.6.1",
              "2.13.2.1",
              "2.14.0"
            ]
          }
        }
      },
      {
        "ref": "pkg:maven/io.quarkus/quarkus-jdbc-postgresql@2.13.5.Final",
        "transitive": [
          {
            "ref": "pkg:maven/org.postgresql/postgresql@42.5.0",
            "issues": [
              {
                "id": "SNYK-JAVA-ORGPOSTGRESQL-3146847",
                "title": "Information Exposure",
                "source": {
                  "provider": "snyk",
                  "origin": "snyk"
                },
                "cvss": {
                  "attackVector": "Local",
                  "attackComplexity": "High",
                  "privilegesRequired": "Low",
                  "userInteraction": "None",
                  "scope": "Unchanged",
                  "confidentialityImpact": "High",
                  "integrityImpact": "None",
                  "availabilityImpact": "None",
                  "cvss": "CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:H/I:N/A:N"
                },
                "cvssScore": 4.7,
                "severity": "MEDIUM",
                "cves": [
                  "CVE-2022-41946"
                ],
                "unique": false,
                "remediation": {
                  "fixedIn": [
                    "42.2.27",
                    "42.3.8",
                    "42.4.3",
                    "42.5.1"
                  ]
                }
              }
            ],
            "highestVulnerability": {
              "id": "SNYK-JAVA-ORGPOSTGRESQL-3146847",
              "title": "Information Exposure",
              "source": {
                "provider": "snyk",
                "origin": "snyk"
              },
              "cvss": {
                "attackVector": "Local",
                "attackComplexity": "High",
                "privilegesRequired": "Low",
                "userInteraction": "None",
                "scope": "Unchanged",
                "confidentialityImpact": "High",
                "integrityImpact": "None",
                "availabilityImpact": "None",
                "cvss": "CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:H/I:N/A:N"
              },
              "cvssScore": 4.7,
              "severity": "MEDIUM",
              "cves": [
                "CVE-2022-41946"
              ],
              "unique": false,
              "remediation": {
                "fixedIn": [
                  "42.2.27",
                  "42.3.8",
                  "42.4.3",
                  "42.5.1"
                ]
              }
            }
          }
        ],
        "highestVulnerability": {
          "id": "SNYK-JAVA-ORGPOSTGRESQL-3146847",
          "title": "Information Exposure",
          "source": {
            "provider": "snyk",
            "origin": "snyk"
          },
          "cvss": {
            "attackVector": "Local",
            "attackComplexity": "High",
            "privilegesRequired": "Low",
            "userInteraction": "None",
            "scope": "Unchanged",
            "confidentialityImpact": "High",
            "integrityImpact": "None",
            "availabilityImpact": "None",
            "cvss": "CVSS:3.1/AV:L/AC:H/PR:L/UI:N/S:U/C:H/I:N/A:N"
          },
          "cvssScore": 4.7,
          "severity": "MEDIUM",
          "cves": [
            "CVE-2022-41946"
          ],
          "unique": false,
          "remediation": {
            "fixedIn": [
              "42.2.27",
              "42.3.8",
              "42.4.3",
              "42.5.1"
            ]
          }
        }
      },
      {
        "ref": "pkg:maven/io.quarkus/quarkus-hibernate-orm@2.13.5.Final",
        "transitive": [
          {
            "ref": "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.13.1",
            "issues": [
              {
                "id": "CVE-2020-36518",
                "title": "[CVE-2020-36518] CWE-787: Out-of-bounds Write",
                "source": {
                  "provider": "oss-index",
                  "origin": "oss-index"
                },
                "cvss": {
                  "attackVector": "Network",
                  "attackComplexity": "Low",
                  "privilegesRequired": "None",
                  "userInteraction": "None",
                  "scope": "Unchanged",
                  "confidentialityImpact": "None",
                  "integrityImpact": "None",
                  "availabilityImpact": "High",
                  "cvss": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
                },
                "cvssScore": 7.5,
                "severity": "HIGH",
                "cves": [
                  "CVE-2020-36518"
                ],
                "unique": false
              },
              {
                "id": "CVE-2022-42003",
                "title": "[CVE-2022-42003] CWE-502: Deserialization of Untrusted Data",
                "source": {
                  "provider": "oss-index",
                  "origin": "oss-index"
                },
                "cvss": {
                  "attackVector": "Network",
                  "attackComplexity": "Low",
                  "privilegesRequired": "None",
                  "userInteraction": "None",
                  "scope": "Unchanged",
                  "confidentialityImpact": "None",
                  "integrityImpact": "None",
                  "availabilityImpact": "High",
                  "cvss": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
                },
                "cvssScore": 7.5,
                "severity": "HIGH",
                "cves": [
                  "CVE-2022-42003"
                ],
                "unique": false
              },
              {
                "id": "CVE-2022-42004",
                "title": "[CVE-2022-42004] CWE-502: Deserialization of Untrusted Data",
                "source": {
                  "provider": "oss-index",
                  "origin": "oss-index"
                },
                "cvss": {
                  "attackVector": "Network",
                  "attackComplexity": "Low",
                  "privilegesRequired": "None",
                  "userInteraction": "None",
                  "scope": "Unchanged",
                  "confidentialityImpact": "None",
                  "integrityImpact": "None",
                  "availabilityImpact": "High",
                  "cvss": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
                },
                "cvssScore": 7.5,
                "severity": "HIGH",
                "cves": [
                  "CVE-2022-42004"
                ],
                "unique": false
              }
            ],
            "highestVulnerability": {
              "id": "CVE-2020-36518",
              "title": "[CVE-2020-36518] CWE-787: Out-of-bounds Write",
              "source": {
                "provider": "oss-index",
                "origin": "oss-index"
              },
              "cvss": {
                "attackVector": "Network",
                "attackComplexity": "Low",
                "privilegesRequired": "None",
                "userInteraction": "None",
                "scope": "Unchanged",
                "confidentialityImpact": "None",
                "integrityImpact": "None",
                "availabilityImpact": "High",
                "cvss": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
              },
              "cvssScore": 7.5,
              "severity": "HIGH",
              "cves": [
                "CVE-2020-36518"
              ],
              "unique": false
            }
          }
        ],
        "highestVulnerability": {
          "id": "CVE-2020-36518",
          "title": "[CVE-2020-36518] CWE-787: Out-of-bounds Write",
          "source": {
            "provider": "oss-index",
            "origin": "oss-index"
          },
          "cvss": {
            "attackVector": "Network",
            "attackComplexity": "Low",
            "privilegesRequired": "None",
            "userInteraction": "None",
            "scope": "Unchanged",
            "confidentialityImpact": "None",
            "integrityImpact": "None",
            "availabilityImpact": "High",
            "cvss": "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H"
          },
          "cvssScore": 7.5,
          "severity": "HIGH",
          "cves": [
            "CVE-2020-36518"
          ],
          "unique": false
        }
      }
    ]
  },
  ossIndexIssueLinkFormatter: {
    issuePathRegex: 'http://ossindex.sonatype.org/vulnerability/%s',
  },
  snykIssueLinkFormatter: {
    issuePathRegex:
      'https://security.snyk.io/vuln/%s?utm_medium=Partner&utm_source=RedHat&utm_campaign=Code-Ready-Analytics-2020&utm_content=vuln/%s',
  },
  sbomPath: 'https://tc-storage-mvp.s3.amazonaws.com/sboms/sbom.json',
  snykSignup:
    'https://app.snyk.io/login?utm_campaign=Code-Ready-Analytics-2020&utm_source=code_ready&code_ready=FF1B53D9-57BE-4613-96D7-1D06066C38C9',
  dependencyHelper: {},
};

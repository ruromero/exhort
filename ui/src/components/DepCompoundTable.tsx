import React from 'react';
import { Divider } from '@patternfly/react-core';
import { Dependency } from '../api/report';
import { GenericCompoundTable, ColumnDef } from './GenericCompoundTable';
import { DependencyLink } from './DependencyLink';
import { TransitiveDependenciesTable } from './TransitiveDependenciesTable';
import { VulnerabilitiesTable } from './VulnerabilitiesTable';
import { VulnerabilitiesCountBySeverity } from './VulnerabilitiesCountBySeverity';
import { extractDependencyVersion } from '../utils/utils';
import { RemediationsAvailability } from './RemediationsAvailability';

export const DepCompoundTable = ({
  name,
  dependencies,
}: {
  name: string;
  dependencies: Dependency[];
}) => {
  const columns: ColumnDef<Dependency>[] = [
    {
      key: 'name',
      header: 'Dependency Name',
      width: 30,
      sortIndex: 1,
      render: (item) => <DependencyLink name={item.ref} />,
    },
    {
      key: 'version',
      header: 'Current Version',
      width: 15,
      render: (item) => extractDependencyVersion(item.ref),
    },
    {
      key: 'direct',
      header: 'Direct Vulnerabilities',
      width: 15,
      compoundExpand: true,
      render: (item) =>
        item.issues?.length ? (
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <div style={{ width: '25px' }}>{item.issues?.length}</div>
            <Divider
              orientation={{ default: 'vertical' }}
              style={{ paddingRight: '10px' }}
            />
            <VulnerabilitiesCountBySeverity vulnerabilities={item.issues} />
          </div>
        ) : (
          0
        ),
    },
    {
      key: 'transitive',
      header: 'Transitive Vulnerabilities',
      width: 15,
      compoundExpand: true,
      render: (item) =>
        item.transitive?.length ? (
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <div style={{ width: '25px' }}>
              {item.transitive
                .map((e) => e.issues?.length)
                .reduce((prev = 0, current = 0) => prev + current)}
            </div>
            <Divider
              orientation={{ default: 'vertical' }}
              style={{ paddingRight: '10px' }}
            />
            <VulnerabilitiesCountBySeverity transitiveDependencies={item.transitive} />
          </div>
        ) : (
          0
        ),
    },
    {
      key: 'rhRemediation',
      header: 'Remediation available',
      width: 15,
      render: (item) => <RemediationsAvailability dependency={item} />,
    },
  ];

  return (
    <GenericCompoundTable<Dependency>
      name={name}
      items={dependencies}
      getRowKey={(item) => item.ref}
      columns={columns}
      filterConfig={{
        placeholder: 'Filter by Dependency name',
        idSuffix: '-dependency-filter',
      }}
      compareToByColumn={(a, b, columnIndex) => {
        switch (columnIndex) {
          case 1:
            return a.ref.localeCompare(b.ref);
          default:
            return 0;
        }
      }}
      filterItem={(item, filterText) => {
        const hasContent = !!(item.issues?.length || item.transitive?.length);
        if (!hasContent) return false;
        if (!filterText || filterText.trim().length === 0) return true;
        return item.ref.toLowerCase().indexOf(filterText.toLowerCase()) !== -1;
      }}
      renderExpandContent={(item, expandedColumnKey) => {
        if (expandedColumnKey === 'direct' && item.issues?.length) {
          return (
            <VulnerabilitiesTable
              providerName={name}
              dependency={item}
              vulnerabilities={item.issues}
            />
          );
        }
        if (expandedColumnKey === 'transitive' && item.transitive?.length) {
          return (
            <TransitiveDependenciesTable
              providerName={name}
              transitiveDependencies={item.transitive}
            />
          );
        }
        return null;
      }}
      ariaLabelPrefix="Dependencies"
      expandId="compound-expandable-example"
      defaultExpanded={{ 'siemur/test-space': 'name' }}
    />
  );
};

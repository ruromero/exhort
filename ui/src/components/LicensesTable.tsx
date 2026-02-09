import React from 'react';
import { Divider, Icon } from '@patternfly/react-core';
import SecurityIcon from '@patternfly/react-icons/dist/esm/icons/security-icon';
import { LicensePackageReport } from '../api/report';
import { getCategoryColor, getCategoryLabel } from './LicensesCountByCategory';
import { GenericCompoundTable, ColumnDef } from './GenericCompoundTable';
import { DependencyLink } from './DependencyLink';
import { LicensesCountByCategory } from './LicensesCountByCategory';
import { extractDependencyVersion } from '../utils/utils';
import { ConcludedLicenseDetail } from './ConcludedLicenseDetail';
import { EvidenceLicensesTable } from './EvidenceLicensesTable';

export interface LicenseTableRow {
  ref: string;
  concluded: LicensePackageReport['concluded'];
  evidence: LicensePackageReport['evidence'];
}

function packagesToRows(packages: {
  [key: string]: LicensePackageReport;
}): LicenseTableRow[] {
  return Object.entries(packages || {}).map(([ref, pkg]) => ({
    ref,
    concluded: pkg.concluded,
    evidence: pkg.evidence || [],
  }));
}

export const LicensesTable = ({
  name,
  dependencies: packages,
}: {
  name: string;
  dependencies: { [key: string]: LicensePackageReport };
}) => {
  const dependencies = packagesToRows(packages);

  const columns: ColumnDef<LicenseTableRow>[] = [
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
      key: 'concluded',
      header: 'Concluded',
      width: 20,
      sortIndex: 2,
      compoundExpand: true,
      render: (item) =>
        item.concluded
          ? item.concluded.expression || item.concluded.name || '—'
          : '—',
    },
    {
      key: 'category',
      header: 'Category',
      width: 15,
      sortIndex: 3,
      render: (item) =>
        item.concluded?.category ? (
          <span>
            <Icon isInline>
              <SecurityIcon
                style={{
                  fill: getCategoryColor(item.concluded.category),
                  height: '13px',
                }}
              />
            </Icon>
            &nbsp;
            {getCategoryLabel(item.concluded.category)}
          </span>
        ) : (
          '—'
        ),
    },
    {
      key: 'licenses',
      header: 'Licenses',
      width: 25,
      compoundExpand: true,
      render: (item) =>
        item.evidence?.length ? (
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <div style={{ width: '25px' }}>{item.evidence.length}</div>
            <Divider orientation={{ default: 'vertical' }} style={{ paddingRight: '10px' }} />
            <LicensesCountByCategory evidence={item.evidence} />
          </div>
        ) : (
          0
        ),
    },
  ];

  return (
    <GenericCompoundTable<LicenseTableRow>
      name={name}
      items={dependencies}
      getRowKey={(item) => item.ref}
      columns={columns}
      filterConfig={{
        placeholder: 'Filter by Dependency name',
        idSuffix: '-license-filter',
      }}
      compareToByColumn={(a, b, columnIndex) => {
        switch (columnIndex) {
          case 1:
            return a.ref.localeCompare(b.ref);
          case 2: {
            const aVal = a.concluded?.expression || a.concluded?.name || '';
            const bVal = b.concluded?.expression || b.concluded?.name || '';
            return aVal.localeCompare(bVal);
          }
          case 3: {
            const aVal = a.concluded?.category || '';
            const bVal = b.concluded?.category || '';
            return aVal.localeCompare(bVal);
          }
          default:
            return 0;
        }
      }}
      filterItem={(item, filterText) => {
        if (!filterText || filterText.trim().length === 0) return true;
        return item.ref.toLowerCase().indexOf(filterText.toLowerCase()) !== -1;
      }}
      renderExpandContent={(item, expandedColumnKey) => {
        if (expandedColumnKey === 'concluded' && item.concluded) {
          return <ConcludedLicenseDetail concluded={item.concluded} />;
        }
        if (expandedColumnKey === 'licenses' && item.evidence?.length) {
          return <EvidenceLicensesTable evidence={item.evidence} />;
        }
        return null;
      }}
      ariaLabelPrefix="Licenses"
      expandId="licenses-compound-expand"
      initialSortBy={{ index: 3, direction: 'desc' }}
    />
  );
};

import React, {useState} from 'react';
import {
  Card,
  CardBody,
  Divider,
  EmptyState,
  EmptyStateBody,
  EmptyStateHeader,
  EmptyStateIcon,
  EmptyStateVariant,
  Icon,
  SearchInput,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  ToolbarItemVariant,
  ToolbarToggleGroup,
} from '@patternfly/react-core';
import {
  ExpandableRowContent,
  Table,
  TableVariant,
  Tbody,
  Td,
  TdProps,
  Th,
  Thead,
  Tr,
} from '@patternfly/react-table';
import FilterIcon from '@patternfly/react-icons/dist/esm/icons/filter-icon';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import SecurityIcon from '@patternfly/react-icons/dist/esm/icons/security-icon';
import {LicensePackageReport} from '../api/report';
import {getCategoryColor, getCategoryLabel} from './LicensesCountByCategory';
import {useTable} from '../hooks/useTable';
import {useTableControls} from '../hooks/useTableControls';
import {SimplePagination} from './TableControls/SimplePagination';
import {DependencyLink} from './DependencyLink';
import {LicensesCountByCategory} from './LicensesCountByCategory';
import {extractDependencyVersion} from '../utils/utils';
import {ConditionalTableBody} from './TableControls/ConditionalTableBody';
import {ConcludedLicenseDetail} from './ConcludedLicenseDetail';
import {EvidenceLicensesTable} from './EvidenceLicensesTable';

export interface LicenseTableRow {
  ref: string;
  concluded: LicensePackageReport['concluded'];
  evidence: LicensePackageReport['evidence'];
}

function packagesToRows(packages: { [key: string]: LicensePackageReport }): LicenseTableRow[] {
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
  const [filterText, setFilterText] = useState('');
  const dependencies = packagesToRows(packages);

  const {
    page: currentPage,
    sortBy: currentSortBy,
    changePage: onPageChange,
    changeSortBy: onChangeSortBy,
  } = useTableControls();

  const {pageItems, filteredItems} = useTable({
    items: dependencies,
    currentPage: currentPage,
    currentSortBy: currentSortBy,
    compareToByColumn: (a: LicenseTableRow, b: LicenseTableRow, columnIndex?: number) => {
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
    },
    filterItem: (item) => {
      if (!filterText || filterText.trim().length === 0) return true;
      return item.ref.toLowerCase().indexOf(filterText.toLowerCase()) !== -1;
    },
  });

  const columnNames = {
    name: 'Dependency Name',
    version: 'Current Version',
    concluded: 'Concluded',
    category: 'Category',
    licenses: 'Licenses',
  };
  type ColumnKey = keyof typeof columnNames;

  const [expandedCells, setExpandedCells] = useState<Record<string, ColumnKey>>({});
  const setCellExpanded = (row: LicenseTableRow, columnKey: ColumnKey, isExpanding = true) => {
    const newExpanded = { ...expandedCells };
    if (isExpanding) {
      newExpanded[row.ref] = columnKey;
    } else {
      delete newExpanded[row.ref];
    }
    setExpandedCells(newExpanded);
  };

  const compoundExpandParams = (
    row: LicenseTableRow,
    columnKey: ColumnKey,
    rowIndex: number,
    columnIndex: number
  ): TdProps['compoundExpand'] => ({
    isExpanded: expandedCells[row.ref] === columnKey,
    onToggle: () => setCellExpanded(row, columnKey, expandedCells[row.ref] !== columnKey),
    expandId: 'licenses-compound-expand',
    rowIndex,
    columnIndex,
  });

  return (
    <Card>
      <CardBody>
        <div style={{backgroundColor: 'var(--pf-v5-global--BackgroundColor--100)'}}>
          <Toolbar>
            <ToolbarContent>
              <ToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
                <ToolbarItem variant="search-filter">
                  <SearchInput
                    id={name + '-license-filter'}
                    style={{width: '250px'}}
                    placeholder="Filter by Dependency name"
                    value={filterText}
                    onChange={(_, value) => setFilterText(value)}
                    onClear={() => setFilterText('')}
                  />
                </ToolbarItem>
              </ToolbarToggleGroup>
              <ToolbarItem variant={ToolbarItemVariant.pagination} align={{default: 'alignRight'}}>
                <SimplePagination
                  isTop={true}
                  count={filteredItems.length}
                  params={currentPage}
                  onChange={onPageChange}
                />
              </ToolbarItem>
            </ToolbarContent>
          </Toolbar>
          <Table aria-label={(name ?? 'Licenses') + ' licenses'} variant={TableVariant.compact}>
            <Thead>
              <Tr>
                <Th
                  width={25}
                  sort={{
                    columnIndex: 1,
                    sortBy: {...currentSortBy},
                    onSort: onChangeSortBy,
                  }}
                >
                  {columnNames.name}
                </Th>
                <Th>{columnNames.version}</Th>
                <Th
                  width={20}
                  sort={{
                    columnIndex: 2,
                    sortBy: {...currentSortBy},
                    onSort: onChangeSortBy,
                  }}
                >
                  {columnNames.concluded}
                </Th>
                <Th
                  width={15}
                  sort={{
                    columnIndex: 3,
                    sortBy: {...currentSortBy},
                    onSort: onChangeSortBy,
                  }}
                >
                  {columnNames.category}
                </Th>
                <Th>{columnNames.licenses}</Th>
              </Tr>
            </Thead>
            <ConditionalTableBody
              isNoData={filteredItems.length === 0}
              numRenderedColumns={5}
              noDataEmptyState={
                <EmptyState variant={EmptyStateVariant.sm}>
                  <EmptyStateHeader
                    icon={<EmptyStateIcon icon={SearchIcon} />}
                    titleText="No results found"
                    headingLevel="h2"
                  />
                  <EmptyStateBody>Clear all filters and try again.</EmptyStateBody>
                </EmptyState>
              }
            >
              {pageItems?.map((item, rowIndex) => {
                const expandedCellKey = expandedCells[item.ref];
                const isRowExpanded = !!expandedCellKey;
                return (
                  <Tbody key={item.ref} isExpanded={isRowExpanded}>
                    <Tr>
                      <Td width={30} dataLabel={columnNames.name} component="th">
                        <DependencyLink name={item.ref} />
                      </Td>
                      <Td width={15} dataLabel={columnNames.version}>
                        {extractDependencyVersion(item.ref)}
                      </Td>
                      <Td
                        width={20}
                        dataLabel={columnNames.concluded}
                        compoundExpand={compoundExpandParams(item, 'concluded', rowIndex, 2)}
                      >
                        {item.concluded ? (
                          item.concluded.expression || item.concluded.name || '—'
                        ) : (
                          '—'
                        )}
                      </Td>
                      <Td width={15} dataLabel={columnNames.category}>
                        {item.concluded?.category ? (
                          <span>
                            <Icon isInline>
                              <SecurityIcon style={{fill: getCategoryColor(item.concluded.category), height: '13px'}} />
                            </Icon>
                            &nbsp;
                            {getCategoryLabel(item.concluded.category)}
                          </span>
                        ) : (
                          '—'
                        )}
                      </Td>
                      <Td
                        width={25}
                        dataLabel={columnNames.licenses}
                        compoundExpand={compoundExpandParams(item, 'licenses', rowIndex, 4)}
                      >
                        {item.evidence?.length ? (
                          <div style={{display: 'flex', alignItems: 'center'}}>
                            <div style={{width: '25px'}}>{item.evidence.length}</div>
                            <Divider
                              orientation={{default: 'vertical'}}
                              style={{paddingRight: '10px'}}
                            />
                            <LicensesCountByCategory evidence={item.evidence} />
                          </div>
                        ) : (
                          0
                        )}
                      </Td>
                    </Tr>
                    {isRowExpanded ? (
                      <Tr isExpanded={isRowExpanded}>
                        <Td dataLabel={columnNames[expandedCellKey]} noPadding colSpan={5}>
                          <ExpandableRowContent>
                            <div className="pf-v5-u-m-md">
                              {expandedCellKey === 'concluded' && item.concluded ? (
                                <ConcludedLicenseDetail concluded={item.concluded} />
                              ) : expandedCellKey === 'licenses' && item.evidence?.length ? (
                                <EvidenceLicensesTable evidence={item.evidence} />
                              ) : null}
                            </div>
                          </ExpandableRowContent>
                        </Td>
                      </Tr>
                    ) : null}
                  </Tbody>
                );
              })}
            </ConditionalTableBody>
          </Table>
          <SimplePagination
            isTop={false}
            count={filteredItems.length}
            params={currentPage}
            onChange={onPageChange}
          />
        </div>
      </CardBody>
    </Card>
  );
};

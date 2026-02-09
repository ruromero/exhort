import React, { useState } from 'react';
import {
  Card,
  CardBody,
  EmptyState,
  EmptyStateBody,
  EmptyStateHeader,
  EmptyStateIcon,
  EmptyStateVariant,
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
import { SortBy } from '../common/types';
import { useTable } from '../hooks/useTable';
import { useTableControls } from '../hooks/useTableControls';
import { SimplePagination } from './TableControls/SimplePagination';
import { ConditionalTableBody } from './TableControls/ConditionalTableBody';

/** PatternFly table column width (percentage) */
export type TableColumnWidth = 10 | 15 | 20 | 25 | 30 | 35 | 40 | 45 | 50 | 60 | 70 | 80 | 90 | 100;

export interface ColumnDef<T> {
  key: string;
  header: string;
  width?: TableColumnWidth;
  /** 1-based column index for sorting (matches PatternFly Table sort columnIndex) */
  sortIndex?: number;
  compoundExpand?: boolean;
  render: (item: T) => React.ReactNode;
}

export interface GenericCompoundTableProps<T> {
  name: string;
  items: T[];
  getRowKey: (item: T) => string;
  columns: ColumnDef<T>[];
  filterConfig: {
    placeholder: string;
    idSuffix: string;
  };
  compareToByColumn: (a: T, b: T, columnIndex?: number) => number;
  filterItem: (item: T, filterText: string) => boolean;
  renderExpandContent: (item: T, expandedColumnKey: string) => React.ReactNode;
  ariaLabelPrefix?: string;
  expandId?: string;
  /** Initial expanded state: record of rowKey -> column key */
  defaultExpanded?: Record<string, string>;
  /** Initial sort (column index is 1-based, matches sortIndex in columns) */
  initialSortBy?: SortBy;
}

export function GenericCompoundTable<T>({
  name,
  items,
  getRowKey,
  columns,
  filterConfig,
  compareToByColumn,
  filterItem,
  renderExpandContent,
  ariaLabelPrefix = 'Table',
  expandId = 'compound-expand-table',
  defaultExpanded = {},
  initialSortBy,
}: GenericCompoundTableProps<T>) {
  const [filterText, setFilterText] = useState('');

  const {
    page: currentPage,
    sortBy: currentSortBy,
    changePage: onPageChange,
    changeSortBy: onChangeSortBy,
  } = useTableControls(initialSortBy ? { sortBy: initialSortBy } : undefined);

  const { pageItems, filteredItems } = useTable({
    items,
    currentPage,
    currentSortBy,
    compareToByColumn,
    filterItem: (item) => filterItem(item, filterText),
  });

  const [expandedCells, setExpandedCells] = useState<Record<string, string>>(defaultExpanded);

  const setCellExpanded = (item: T, columnKey: string, isExpanding = true) => {
    const rowKey = getRowKey(item);
    const newExpanded = { ...expandedCells };
    if (isExpanding) {
      newExpanded[rowKey] = columnKey;
    } else {
      delete newExpanded[rowKey];
    }
    setExpandedCells(newExpanded);
  };

  const compoundExpandParams = (
    item: T,
    columnKey: string,
    rowIndex: number,
    columnIndex: number
  ): TdProps['compoundExpand'] => ({
    isExpanded: expandedCells[getRowKey(item)] === columnKey,
    onToggle: () =>
      setCellExpanded(item, columnKey, expandedCells[getRowKey(item)] !== columnKey),
    expandId,
    rowIndex,
    columnIndex,
  });

  const numColumns = columns.length;

  return (
    <Card>
      <CardBody>
        <div style={{ backgroundColor: 'var(--pf-v5-global--BackgroundColor--100)' }}>
          <Toolbar>
            <ToolbarContent>
              <ToolbarToggleGroup toggleIcon={<FilterIcon />} breakpoint="xl">
                <ToolbarItem variant="search-filter">
                  <SearchInput
                    id={name + filterConfig.idSuffix}
                    style={{ width: '250px' }}
                    placeholder={filterConfig.placeholder}
                    value={filterText}
                    onChange={(_, value) => setFilterText(value)}
                    onClear={() => setFilterText('')}
                  />
                </ToolbarItem>
              </ToolbarToggleGroup>
              <ToolbarItem variant={ToolbarItemVariant.pagination} align={{ default: 'alignRight' }}>
                <SimplePagination
                  isTop={true}
                  count={filteredItems.length}
                  params={currentPage}
                  onChange={onPageChange}
                />
              </ToolbarItem>
            </ToolbarContent>
          </Toolbar>
          <Table
            aria-label={(name ?? ariaLabelPrefix) + ' table'}
            variant={TableVariant.compact}
          >
            <Thead>
              <Tr>
                {columns.map((col, idx) => (
                  <Th
                    key={col.key}
                    width={col.width}
                    sort={
                      col.sortIndex != null
                        ? {
                            columnIndex: col.sortIndex,
                            sortBy: { ...currentSortBy },
                            onSort: onChangeSortBy,
                          }
                        : undefined
                    }
                  >
                    {col.header}
                  </Th>
                ))}
              </Tr>
            </Thead>
            <ConditionalTableBody
              isNoData={filteredItems.length === 0}
              numRenderedColumns={numColumns}
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
                const rowKey = getRowKey(item);
                const expandedCellKey = expandedCells[rowKey];
                const isRowExpanded = !!expandedCellKey;
                return (
                  <Tbody key={rowKey} isExpanded={isRowExpanded}>
                    <Tr>
                      {columns.map((col, colIndex) => (
                        <Td
                          key={col.key}
                          width={col.width}
                          dataLabel={col.header}
                          component={colIndex === 0 ? 'th' : undefined}
                          compoundExpand={
                            col.compoundExpand
                              ? compoundExpandParams(item, col.key, rowIndex, colIndex)
                              : undefined
                          }
                        >
                          {col.render(item)}
                        </Td>
                      ))}
                    </Tr>
                    {isRowExpanded && expandedCellKey ? (
                      <Tr isExpanded={true}>
                        <Td
                          dataLabel={columns.find((c) => c.key === expandedCellKey)?.header}
                          noPadding
                          colSpan={numColumns}
                        >
                          <ExpandableRowContent>
                            <div className="pf-v5-u-m-md">
                              {renderExpandContent(item, expandedCellKey)}
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
}

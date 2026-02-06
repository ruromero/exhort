import {useMemo} from 'react';
import {Card, Icon} from '@patternfly/react-core';
import {Table, TableVariant, Tbody, Td, Th, Thead, Tr} from '@patternfly/react-table';
import SecurityIcon from '@patternfly/react-icons/dist/esm/icons/security-icon';
import {LicenseInfo} from '../api/report';
import {getCategoryColor, getCategoryLabel, getCategorySortIndex} from './LicensesCountByCategory';
import {ConditionalTableBody} from './TableControls/ConditionalTableBody';

const columnNames = {
  evidence: 'Evidence',
  expression: 'Expression',
  category: 'Category',
};

export const EvidenceLicensesTable = ({
  evidence = [],
}: {
  evidence: LicenseInfo[];
}) => {
  const sortedEvidence = useMemo(
    () => [...evidence].sort((a, b) => getCategorySortIndex(a.category) - getCategorySortIndex(b.category)),
    [evidence]
  );

  return (
    <Card
      style={{
        backgroundColor: 'var(--pf-v5-global--BackgroundColor--100)',
      }}
    >
      <Table variant={TableVariant.compact} aria-label="Evidence licenses">
        <Thead>
          <Tr>
            <Th width={40}>{columnNames.evidence}</Th>
            <Th width={30}>{columnNames.expression}</Th>
            <Th width={30}>{columnNames.category}</Th>
          </Tr>
        </Thead>
        <ConditionalTableBody isNoData={sortedEvidence.length === 0} numRenderedColumns={3}>
          <Tbody>
            {sortedEvidence.map((info, rowIndex) => {
              const evidenceLabel = info.name || info.identifiers?.join(', ') || info.expression || '—';
              const expression = info.expression || '—';
              const category = info.category || '—';
              const color = getCategoryColor(info.category);
              return (
                <Tr key={rowIndex}>
                  <Td dataLabel={columnNames.evidence}>{evidenceLabel}</Td>
                  <Td dataLabel={columnNames.expression}>{expression}</Td>
                  <Td dataLabel={columnNames.category}>
                    {category !== '—' ? (
                      <span>
                        <Icon isInline>
                          <SecurityIcon style={{fill: color, height: '13px'}} />
                        </Icon>
                        &nbsp;
                        {getCategoryLabel(info.category)}
                      </span>
                    ) : (
                      '—'
                    )}
                  </Td>
                </Tr>
              );
            })}
          </Tbody>
        </ConditionalTableBody>
      </Table>
    </Card>
  );
};

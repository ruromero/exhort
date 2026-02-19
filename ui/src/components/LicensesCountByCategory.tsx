import React from 'react';
import {FlexItem, Icon} from '@patternfly/react-core';
import {LicenseInfo} from '../api/report';
import SecurityIcon from '@patternfly/react-icons/dist/esm/icons/security-icon';

// Same order and colors as LicensesChartCard: permissive, weak copyleft, strong copyleft, unknown
export const CATEGORY_COLORS: Record<string, string> = {
  PERMISSIVE: '#0066CC',
  WEAK_COPYLEFT: '#3E8635',
  STRONG_COPYLEFT: '#F0AB00',
  UNKNOWN: '#C46100',
};

export const CATEGORY_LABELS: Record<string, string> = {
  PERMISSIVE: 'Permissive',
  WEAK_COPYLEFT: 'Weak copyleft',
  STRONG_COPYLEFT: 'Strong copyleft',
  UNKNOWN: 'Unknown',
};

export function getCategoryLabel(category: string | undefined): string {
  if (!category) return CATEGORY_LABELS.UNKNOWN;
  const cat = category.toUpperCase().replace(/-/g, '_');
  return CATEGORY_LABELS[cat] ?? category;
}

export function getCategoryColor(category: string | undefined): string {
  if (!category) return CATEGORY_COLORS.UNKNOWN;
  const cat = category.toUpperCase().replace(/-/g, '_');
  return CATEGORY_COLORS[cat] ?? CATEGORY_COLORS.UNKNOWN;
}

/** Sort order by decreasing permissiveness: Permissive first, then Weak copyleft, Strong copyleft, Unknown last. */
export const CATEGORY_SORT_ORDER = ['PERMISSIVE', 'WEAK_COPYLEFT', 'STRONG_COPYLEFT', 'UNKNOWN'] as const;

export function getCategorySortIndex(category: string | undefined): number {
  if (!category) return CATEGORY_SORT_ORDER.length;
  const cat = category.toUpperCase().replace(/-/g, '_');
  const idx = CATEGORY_SORT_ORDER.indexOf(cat as (typeof CATEGORY_SORT_ORDER)[number]);
  return idx >= 0 ? idx : CATEGORY_SORT_ORDER.length;
}

const CATEGORY_ORDER = CATEGORY_SORT_ORDER;

function countByCategory(evidence: LicenseInfo[]): Record<string, number> {
  const counts: Record<string, number> = {
    PERMISSIVE: 0,
    WEAK_COPYLEFT: 0,
    STRONG_COPYLEFT: 0,
    UNKNOWN: 0,
  };
  evidence?.forEach((info) => {
    const cat = (info.category || 'UNKNOWN').toUpperCase().replace(/-/g, '_');
    if (counts.hasOwnProperty(cat)) {
      counts[cat]++;
    } else {
      counts.UNKNOWN++;
    }
  });
  return counts;
}

function countByIdentifierCategory(evidence: LicenseInfo[]): Record<string, number> {
  const counts: Record<string, number> = {
    PERMISSIVE: 0,
    WEAK_COPYLEFT: 0,
    STRONG_COPYLEFT: 0,
    UNKNOWN: 0,
  };
  evidence?.forEach((info) => {
    (info.identifiers || []).forEach((id) => {
      const cat = (id.category || 'UNKNOWN').toUpperCase().replace(/-/g, '_');
      if (counts.hasOwnProperty(cat)) {
        counts[cat]++;
      } else {
        counts.UNKNOWN++;
      }
    });
  });
  return counts;
}

export const LicensesCountByCategory = ({
  evidence = [],
  countBy = 'evidence',
}: {
  evidence: LicenseInfo[];
  /** 'evidence': count by each evidence's category; 'identifiers': count by each identifier's category across all evidences */
  countBy?: 'evidence' | 'identifiers';
}) => {
  const counts = countBy === 'identifiers' ? countByIdentifierCategory(evidence) : countByCategory(evidence);

  return (
    <FlexItem>
      {CATEGORY_ORDER.map(
        (cat) =>
          counts[cat] > 0 && (
            <React.Fragment key={cat}>
              <Icon isInline>
                <SecurityIcon style={{fill: CATEGORY_COLORS[cat], height: '13px'}} />
              </Icon>
              &nbsp;
              {counts[cat]}&nbsp;
            </React.Fragment>
          )
      )}
    </FlexItem>
  );
};

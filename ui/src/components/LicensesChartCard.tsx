import {Bullseye, CardBody} from '@patternfly/react-core';
import {ChartDonut, ChartThemeColor} from '@patternfly/react-charts';
import {LicenseSummary} from '../api/report';

const customColors = [ChartThemeColor.blue,ChartThemeColor.green,ChartThemeColor.gold,ChartThemeColor.orange];

export const LicensesChartCard = ({summary}: { summary: LicenseSummary }) => {

  const permissive = summary["permissive"] ?? 0;
  const strongCopyleft = summary["strong-copyleft"] ?? 0;
  const unknown = summary["unknown"] ?? 0;
  const weakCopyleft = summary["weak-copyleft"] ?? 0;
  const concluded = summary["concluded"] ?? 0;

  const hasValues = permissive + strongCopyleft + unknown + weakCopyleft > 0;
  const zeroColor = '#D5F5E3';
  const colorScale = hasValues ? customColors : [zeroColor];

  const legendData = [
    {name: `Permissive: ${permissive}`, symbol: {type: 'square', fill: customColors[0]}},
    {name: `Weak Copyleft: ${weakCopyleft}`, symbol: {type: 'square', fill: customColors[1]}},
    {name: `Strong Copyleft: ${strongCopyleft}`, symbol: {type: 'square', fill: customColors[2]}},
    {name: `Unknown: ${unknown}`, symbol: {type: 'square', fill: customColors[3]}},
  ];

  return (
    <div>
      {/* Chart */}
      <CardBody style={{paddingBottom: "inherit", padding: "0"}}>
        <Bullseye>
          <div style={{height: '230px', width: '350px'}}>
            <ChartDonut
              constrainToVisibleArea
              data={hasValues ? [
                {x: 'Permissive', y: permissive},
                {x: 'Weak Copyleft', y: weakCopyleft},
                {x: 'Strong Copyleft', y: strongCopyleft},
                {x: 'Unknown', y: unknown},
              ] : [{x: 'Empty', y: 1e-10}]}
              labels={({datum}) => hasValues ? `${datum.x}: ${datum.y}` : 'No licenses'}
              legendData={legendData}
              legendOrientation="vertical"
              legendPosition="right"
              padding={{
                left: 0,
                right: 160, // Adjusted to accommodate legend
              }}
              subTitle="Concluded licenses"
              title={`${concluded}`}
              width={350}
              colorScale={colorScale}
            />
          </div>
        </Bullseye>
      </CardBody>
    </div>
  );
};

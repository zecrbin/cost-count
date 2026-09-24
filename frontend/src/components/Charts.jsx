import { Center, Loader } from '@mantine/core';
import { Suspense, lazy } from 'react';

// 图表库（recharts）体积较大，按需加载：页面框架和数字先显示，图表随后出现。
const load = (name) => lazy(() => import('@mantine/charts').then((module) => ({ default: module[name] })));
const LazyBarChart = load('BarChart');
const LazyDonutChart = load('DonutChart');
const LazyAreaChart = load('AreaChart');

function withFallback(Component) {
  return function ChartWithFallback(props) {
    const height = props.h ?? props.size ?? 200;
    return (
      <Suspense fallback={<Center h={height} w={props.size}><Loader type="dots" size="sm" /></Center>}>
        <Component {...props} />
      </Suspense>
    );
  };
}

export const BarChart = withFallback(LazyBarChart);
export const DonutChart = withFallback(LazyDonutChart);
export const AreaChart = withFallback(LazyAreaChart);

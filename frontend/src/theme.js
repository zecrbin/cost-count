import { createTheme } from '@mantine/core';

const fontFamily = '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", '
  + '"Microsoft YaHei", "Noto Sans SC", "Helvetica Neue", Arial, sans-serif';

export const theme = createTheme({
  primaryColor: 'indigo',
  primaryShade: { light: 7, dark: 5 },
  fontFamily,
  fontFamilyMonospace: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
  headings: { fontFamily, fontWeight: '650' },
  defaultRadius: 'md',
  cursorType: 'pointer',
  // 深色模式使用偏冷的灰阶，与浅色模式的中性灰对应。
  colors: {
    dark: ['#d5d8de', '#aeb3bd', '#8a909c', '#6b717d', '#454a53', '#30343b', '#22252b', '#1a1c21', '#131519', '#0d0e11'],
  },
  components: {
    Card: { defaultProps: { radius: 'lg', withBorder: true, padding: 'lg' } },
    Paper: { defaultProps: { radius: 'lg' } },
    Modal: { defaultProps: { radius: 'lg', centered: true, overlayProps: { backgroundOpacity: 0.4, blur: 3 } } },
    Drawer: { defaultProps: { overlayProps: { backgroundOpacity: 0.3, blur: 2 } } },
    Button: { defaultProps: { radius: 'md' } },
    TextInput: { defaultProps: { radius: 'md' } },
    NumberInput: { defaultProps: { radius: 'md' } },
    Select: { defaultProps: { radius: 'md', comboboxProps: { shadow: 'md' } } },
    SegmentedControl: { defaultProps: { radius: 'md' } },
    Badge: { defaultProps: { radius: 'sm', variant: 'light' } },
    Tooltip: { defaultProps: { withArrow: true, openDelay: 250 } },
  },
});

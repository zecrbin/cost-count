import '@fontsource-variable/nunito';
import { createTheme } from '@mantine/core';

// 数字和英文使用圆润的 Nunito（随包本地加载），中文回退到系统字体。
const fontFamily = '"Nunito Variable", -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", '
  + '"Microsoft YaHei", "Noto Sans SC", "Segoe UI", sans-serif';

export const theme = createTheme({
  primaryColor: 'berry',
  primaryShade: { light: 6, dark: 4 },
  fontFamily,
  headings: { fontFamily, fontWeight: '800' },
  defaultRadius: 'lg',
  cursorType: 'pointer',
  colors: {
    // 糖果紫：主色
    berry: ['#f6f2ff', '#ece4ff', '#d8c8ff', '#c2a9ff', '#ab8bfb', '#9a74f8', '#8b5cf6', '#7646dc', '#6338bd', '#51309a'],
    // 夜间糖果：深色模式的紫调灰阶
    dark: ['#f1eeff', '#c9c3e6', '#a49dc6', '#7f78a3', '#4a4470', '#352f58', '#2a2548', '#221e3b', '#1a1730', '#131124'],
  },
  components: {
    Card: { defaultProps: { radius: 'xl', padding: 'lg' } },
    Paper: { defaultProps: { radius: 'xl' } },
    Modal: { defaultProps: { radius: 28, centered: true, overlayProps: { backgroundOpacity: 0.35, blur: 4 } } },
    Drawer: { defaultProps: { overlayProps: { backgroundOpacity: 0.3, blur: 3 } } },
    Button: { defaultProps: { radius: 'xl' } },
    ActionIcon: { defaultProps: { radius: 'xl' } },
    TextInput: { defaultProps: { radius: 'lg' } },
    NumberInput: { defaultProps: { radius: 'lg' } },
    Select: { defaultProps: { radius: 'lg', comboboxProps: { shadow: 'md', radius: 'lg' } } },
    SegmentedControl: { defaultProps: { radius: 'xl' } },
    Badge: { defaultProps: { radius: 'xl', variant: 'light' } },
    Tooltip: { defaultProps: { withArrow: true, openDelay: 250, radius: 'md' } },
    Menu: { defaultProps: { radius: 'lg' } },
    Popover: { defaultProps: { radius: 'lg' } },
    Progress: { defaultProps: { radius: 'xl' } },
    Notification: { defaultProps: { radius: 'lg' } },
  },
});

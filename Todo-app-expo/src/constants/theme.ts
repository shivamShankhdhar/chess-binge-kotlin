/**
 * Below are the colors that are used in the app. The colors are defined in the light and dark mode.
 * There are many other ways to style your app. For example, [Nativewind](https://www.nativewind.dev/), [Tamagui](https://tamagui.dev/), [unistyles](https://reactnativeunistyles.vercel.app), etc.
 */

import '@/global.css';

import { Platform } from 'react-native';

export const Colors = {
  light: {
    background: '#F8F7FC',
    surface: '#FFFFFF',
    elevated: '#F4F1FA',
    text: '#24202D',
    muted: '#777181',
    border: '#E9E5F0',
    purple: '#7C4DFF',
    purpleSurface: '#EEE7FF',
    green: '#62C79B',
    greenSurface: '#E4F5ED',
    danger: '#F26B6B',
    dangerSurface: '#FFE8E8',
    warning: '#FFC94A',
    blue: '#62B9F5',
    onPurple: '#FFFFFF',
    backgroundElement: '#FFFFFF',
    backgroundSelected: '#EEE7FF',
    textSecondary: '#777181',
    lavender: '#EEE7FF',
    ink: '#24202D',
    mint: '#E4F5ED',
    coral: '#FFE8E8',
  },
  dark: {
    background: '#000000',
    surface: '#1D1925',
    elevated: '#25202F',
    text: '#F8F5FF',
    muted: '#AAA3B5',
    border: '#332D3D',
    purple: '#B5A1FF',
    purpleSurface: '#352C50',
    green: '#83D9AE',
    greenSurface: '#233B31',
    danger: '#FF9898',
    dangerSurface: '#4A292F',
    warning: '#FFD269',
    blue: '#82C9FA',
    onPurple: '#201735',
    backgroundElement: '#1D1925',
    backgroundSelected: '#352C50',
    textSecondary: '#AAA3B5',
    lavender: '#352C50',
    ink: '#F8F5FF',
    mint: '#233B31',
    coral: '#4A292F',
  },
} as const;

export type ThemeColor = keyof typeof Colors.light & keyof typeof Colors.dark;

export const Fonts = Platform.select({
  ios: {
    /** iOS `UIFontDescriptorSystemDesignDefault` */
    sans: 'system-ui',
    /** iOS `UIFontDescriptorSystemDesignSerif` */
    serif: 'ui-serif',
    /** iOS `UIFontDescriptorSystemDesignRounded` */
    rounded: 'ui-rounded',
    /** iOS `UIFontDescriptorSystemDesignMonospaced` */
    mono: 'ui-monospace',
  },
  default: {
    sans: 'normal',
    serif: 'serif',
    rounded: 'normal',
    mono: 'monospace',
  },
  web: {
    sans: 'var(--font-display)',
    serif: 'var(--font-serif)',
    rounded: 'var(--font-rounded)',
    mono: 'var(--font-mono)',
  },
});

export const Spacing = {
  half: 2,
  one: 4,
  two: 8,
  three: 16,
  four: 24,
  five: 32,
  six: 64,
} as const;

export const BottomTabInset = Platform.select({ ios: 50, android: 80 }) ?? 0;
export const MaxContentWidth = 800;

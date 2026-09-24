import '@mantine/core/styles.css';
import '@mantine/charts/styles.css';
import '@mantine/notifications/styles.css';
import './styles.css';

import { MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { DataProvider } from './lib/data';
import { TransactionEditorProvider } from './lib/editor';
import { theme } from './theme';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <MantineProvider theme={theme} defaultColorScheme="auto">
      <Notifications position="top-center" />
      <DataProvider>
        <TransactionEditorProvider>
          <App />
        </TransactionEditorProvider>
      </DataProvider>
    </MantineProvider>
  </StrictMode>,
);

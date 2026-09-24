import React from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/noto-serif-sc/600.css';
import '@fontsource/noto-serif-sc/700.css';
import '@fontsource-variable/fraunces/full.css';
import './styles.css';
import { StoreProvider } from './lib/store';
import { App } from './App';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <StoreProvider>
      <App />
    </StoreProvider>
  </React.StrictMode>,
);

import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';

import $cssFromFile_0 from '@vaadin/vaadin-lumo-styles/lumo.css?inline';

injectGlobalWebcomponentCss($cssFromFile_0.toString());
import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/horizontal-layout/src/vaadin-horizontal-layout.js';
import '@vaadin/radio-group/src/vaadin-radio-group.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import 'Frontend/generated/jar-resources/flow-component-directive.js';
import 'lit';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import 'lit/directives/live.js';
import '@vaadin/radio-group/src/vaadin-radio-button.js';
import '@vaadin/button/src/vaadin-button.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/notification/src/vaadin-notification.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/password-field/src/vaadin-password-field.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';
import 'react-router';
import 'react';
const loadOnDemand = (key) => {
  const pending = [];
  if (key === '9c92cc019c51c70527d8e7f4af0335ea5ed6ce9d7d10aed53a90e80125a5efac') {
    pending.push(import('./chunks/chunk-5644a418368b23cd16fa9057ff5bd62b36559a779df006fca8dc92cd73f00631.js'));
  }
  if (key === '15886dd6236e8e4f3cf8807e8b3aa0d8f4f71df6d5ee09c37a4a33c6fbe7b808') {
    pending.push(import('./chunks/chunk-7176cdf5cb7fc32e5beb57a0f105f4e37c8d90a2f8a7ed879afa85e91cde605b.js'));
  }
  return Promise.all(pending);
}
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}
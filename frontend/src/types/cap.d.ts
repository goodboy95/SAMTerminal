import React from 'react';

declare global {
  namespace JSX {
    interface IntrinsicElements {
      'cap-widget': React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement> & {
        'data-cap-api-endpoint'?: string;
        'data-cap-hidden-field-name'?: string;
        'data-cap-floating'?: string;
        'data-cap-floating-position'?: string;
      };
    }
  }
}

export {};

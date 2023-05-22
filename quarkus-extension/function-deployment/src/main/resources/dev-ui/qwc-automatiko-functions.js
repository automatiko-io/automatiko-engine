import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { functions } from 'build-time-data';
import '@vaadin/grid';
import '@vaadin/vertical-layout';
import 'qui-badge';

export class QwcAutomatikoFunctions extends LitElement {

    static styles = css`
        .arctable {
          height: 100%;
          padding-bottom: 10px;
        }

        code {
          font-size: 85%;
        }

        .annotation {
          color: var(--lumo-contrast-50pct);
        }

        .producer {
          color: var(--lumo-primary-text-color);
        }
        `;

    static properties = {
        _functions: {state: true},
    };

    constructor() {
        super();
        this._functions = functions;
    }
    
     textToClipboard (text) {
	    var dummy = document.createElement("textarea");
	    document.body.appendChild(dummy);
	    dummy.value = text;
	    dummy.select();
	    document.execCommand("copy");
	    document.body.removeChild(dummy);
	}

    render() {
        if (this._functions) {

            return html`
                <vaadin-grid .items="${this._functions}" class="arctable" theme="no-border">
                    <vaadin-grid-column auto-width
                        header="Name"
                        ${columnBodyRenderer(this._nameRenderer, [])}
                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                        header="Endpoint"
                        ${columnBodyRenderer(this._endpointRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    
                    <vaadin-grid-column auto-width
                        header="POST"
                        ${columnBodyRenderer(this._postRenderer, [])}
                        resizable>
                    </vaadin-grid-column>  
                     <vaadin-grid-column auto-width
                        header=""
                        ${columnBodyRenderer(this._postCopyRenderer, [])}
                        >
                    </vaadin-grid-column>  
                    
                    <vaadin-grid-column auto-width
                        header="GET"
                        ${columnBodyRenderer(this._getRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header=""
                        ${columnBodyRenderer(this._getCopyRenderer, [])}
                        >
                    </vaadin-grid-column>
                  
                </vaadin-grid>`;
            
        } else {
            return html`No functions found`;
        }
    }


    _nameRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          ${func.name}
        </vaadin-vertical-layout>
    `;
    }
    
    _endpointRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          ${func.endpoint}
        </vaadin-vertical-layout>
    `;
    }
    _getRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          <pre>${func.getInstructions}</pre>
        </vaadin-vertical-layout>
    `;
    }
	_getCopyRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          <vaadin-button theme="small" @click=${() => this.textToClipboard(func.curlGet)} class="button">
          	<vaadin-icon class="clearIcon" icon="font-awesome-solid:copy"></vaadin-icon>
          </vaadin-button>           
        </vaadin-vertical-layout>
    `;
    }
    _postRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          <pre>${func.postInstructions}</pre>
        </vaadin-vertical-layout>
    `;
    }   
    _postCopyRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          <vaadin-button theme="small" @click=${() => this.textToClipboard(func.curlPost)} class="button">
          	<vaadin-icon class="clearIcon" icon="font-awesome-solid:copy"></vaadin-icon>
          </vaadin-button>         
        </vaadin-vertical-layout>
    `;
    }   
}
customElements.define('qwc-automatiko-functions', QwcAutomatikoFunctions);
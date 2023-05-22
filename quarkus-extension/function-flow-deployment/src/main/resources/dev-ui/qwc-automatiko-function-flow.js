import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { functionFlows } from 'build-time-data';
import '@vaadin/grid';
import '@vaadin/vertical-layout';
import 'qui-badge';

export class QwcAutomatikoFunctionFlow extends LitElement {

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
        _functionFlows: {state: true},
    };

    constructor() {
        super();
        this._functionFlows = functionFlows;
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
        if (this._functionFlows) {

            return html`
                <vaadin-grid .items="${this._functionFlows}" class="arctable" theme="no-border">
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
                        header="Cloud event - Binary"
                        ${columnBodyRenderer(this._binaryRenderer, [])}
                        resizable>
                    </vaadin-grid-column>  
                    <vaadin-grid-column auto-width
                        header=""
                        ${columnBodyRenderer(this._binaryCopyRenderer, [])}
                        >
                    </vaadin-grid-column> 
                    
                    <vaadin-grid-column auto-width
                        header="Cloud event - Structured"
                        ${columnBodyRenderer(this._structureRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                    <vaadin-grid-column auto-width
                        header=""
                        ${columnBodyRenderer(this._structureCopyRenderer, [])}
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
    _binaryRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          <pre>${func.binaryInstructions}</pre>
        </vaadin-vertical-layout>
    `;
    }
    _binaryCopyRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          <vaadin-button theme="small" @click=${() => this.textToClipboard(func.curlBinary)} class="button">
          	<vaadin-icon class="clearIcon" icon="font-awesome-solid:copy"></vaadin-icon>
          </vaadin-button> 
        </vaadin-vertical-layout>
    `;
    }
    _structureRenderer(func) {
      return html`
        <vaadin-vertical-layout>       
          <pre>${func.structuredInstructions}</pre>
        </vaadin-vertical-layout>
    `;
    }  
    _structureCopyRenderer(func) {
      return html`
        <vaadin-vertical-layout>
          <vaadin-button theme="small" @click=${() => this.textToClipboard(func.curlStructure)} class="button">
          	<vaadin-icon class="clearIcon" icon="font-awesome-solid:copy"></vaadin-icon>
          </vaadin-button>           
        </vaadin-vertical-layout>
    `;
    }    
}
customElements.define('qwc-automatiko-function-flow', QwcAutomatikoFunctionFlow);
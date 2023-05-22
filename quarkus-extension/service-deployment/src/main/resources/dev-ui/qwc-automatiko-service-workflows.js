import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/vertical-layout';
import 'qui-badge';


export class QwcAutomatikoServiceWorkflows extends LitElement {
	
	jsonRpc = new JsonRpc("AutomatikoService");

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
        _workflows: {state: true},
    };
    
    connectedCallback() {
        super.connectedCallback();
        console.log("connected callback")
        this.jsonRpc.getInfo().then(jsonRpcResponse => {
            this._workflows = [];
            jsonRpcResponse.result.forEach(c => {
                this._workflows.push(c);
            });
        });
    }

    render() {
        if (this._workflows) {

            return html`
                <vaadin-grid .items="${this._workflows}" class="arctable" theme="no-border">
                    <vaadin-grid-column auto-width
                        header="ID"
                        ${columnBodyRenderer(this._idRenderer, [])}
                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                        header="Name"
                        ${columnBodyRenderer(this._nameRenderer, [])}
                        resizable>
                    </vaadin-grid-column>

                    <vaadin-grid-column auto-width
                        header="Description"
                        ${columnBodyRenderer(this._descriptionRenderer, [])}
                        resizable>
                    </vaadin-grid-column>

	                <vaadin-grid-column auto-width
                        header="Is public"
                        ${columnBodyRenderer(this._visibilityRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>`;
            
        } else {
            return html`No workflows found`;
        }
    }

    _idRenderer(workflows) {
        return html`<vaadin-vertical-layout>
      ${workflows.id}
      </vaadin-vertical-layout>`;
    }

    _nameRenderer(workflows) {
        return html`<vaadin-vertical-layout>
      ${workflows.name}
      </vaadin-vertical-layout>`;
    }
    
    _descriptionRenderer(workflows) {
        return html`<vaadin-vertical-layout>
      ${workflows.description}
      </vaadin-vertical-layout>`;
    }
    
    _visibilityRenderer(workflows) {
        return html`<vaadin-vertical-layout>
      ${workflows.publicProcess}
      </vaadin-vertical-layout>`;
    }
}
customElements.define('qwc-automatiko-service-workflows', QwcAutomatikoServiceWorkflows);
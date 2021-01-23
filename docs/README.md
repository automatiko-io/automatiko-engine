<p align="center">
    <img src="img/automatiko-documentation.png" width="700px" alt="Automatiko Documentation"/>
</p>

<p align="center">
    Documentation for the Automatiko Platform
</p>

## How to build locally

1. Install Antora

The easiest way is to follow official guide that can be found at [this link](https://docs.antora.org/antora/2.3/install/install-antora/)

In general it comes down to installing it via `npm`

```
npm i -g @antora/cli@2.3 @antora/site-generator-default@2.3

antora -v

```

2. Build the documentation site

```
antora antora-playbook.yml
```

3. Preview the site

Open any browser and point it to `build/site/index.html`

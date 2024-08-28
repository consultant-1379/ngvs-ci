# installing the dependencies (custom step, requested by CUS UI Assets)
npm install

# removing the '.tmp' directory, produced by performing 'npm install', to remove temporary files
rm -rf .tmp

# building the project (custom step, requested by CUS UI Assets)
npm run build

# perform demo deployment (custom steps, requested by CUS UI Assets)
npm run build:demo
rm -rf /workarea/artifacts/npm-sites/cus/demo
mkdir -p /workarea/artifacts/npm-sites/cus/demo
cp -R dist/* /workarea/artifacts/npm-sites/cus/demo/

# perform docs deployment (custom steps, requested by CUS UI Assets)
npm run build:docs
rm -rf /workarea/artifacts/npm-sites/cus/docs
mkdir -p /workarea/artifacts/npm-sites/cus/docs
cp -R dist/* /workarea/artifacts/npm-sites/cus/docs/

# custom step, requested by CUS UI Assets
. \${WORKSPACE}/ci_scripts/custom_prepublish_step.sh
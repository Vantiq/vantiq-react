const path = require('path');
const pkg = require('../package.json');

module.exports = {
  project: {
    ios: {
    },
  },
  dependencies: {
    [pkg.name]: {
      root: path.join(__dirname, '..'),
    },
  },
};

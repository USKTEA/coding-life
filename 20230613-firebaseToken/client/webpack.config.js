const path = require('path');

module.exports = {
  entry: './src/firebase.js',
  devtool: 'eval-source-map',
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'bundle.js',
  },
};

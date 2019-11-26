var Ajv = require('ajv/dist/ajv.bundle');
var ajv = new Ajv();

var console = {};
console.assert = console.clear = console.constructor = console.count = console.debug = console.dir = console.dirxml
    = console.error = console.group = console.groupCollapsed = console.groupEnd = console.info = console.log
    = console.markTimeline = console.profile = console.profileEnd = console.table = console.time = console.timeEnd
    = console.timeStamp = console.timeline = console.timelineEnd = console.trace = console.warn = function () {
};

function validate($id, value, encoded) {
  if (encoded) {
    value = JSON.parse(value);
  }
  var result = {};
  result.isValid = ajv.validate($id, value);
  result.errors = ajv.errors;
  return result;
}

function addSchema(json) {
  var schema = JSON.parse(json);
  if (!schema.$id) {
    throw '$id cannot be null';
  }
  ajv.addSchema(schema);
}

function removeSchema($id) {
  ajv.removeSchema($id);
}

function containsSchema($id) {
  return !!ajv.getSchema($id);

}
function changeStations(inp) {
  inp.addEventListener("click", function(e) {
      var fromField = document.getElementById("from");
      var fromValue = fromField.value;
      var toField = document.getElementById("to");
      var toValue = toField.value;
      fromField.value = toValue;
      toField.value = fromValue;
  });
}

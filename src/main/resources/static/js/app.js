// Close the context switcher when clicking elsewhere
document.addEventListener('click', function (e) {
  document.querySelectorAll('details.switch[open]').forEach(function (d) {
    if (!d.contains(e.target)) d.removeAttribute('open');
  });
});
// Show the live value next to range sliders
document.querySelectorAll('input[type=range][data-out]').forEach(function (r) {
  var out = document.getElementById(r.dataset.out);
  var update = function () { if (out) out.textContent = r.value; };
  r.addEventListener('input', update); update();
});

// Done/Partly/Missed segments: clicking the already-selected option clears it back to "not logged".
(function () {
  document.querySelectorAll('.segs input[type=radio]').forEach(function (radio) {
    radio.addEventListener('mousedown', function () {
      radio.dataset.wasChecked = radio.checked ? 'true' : 'false';
    });
    radio.addEventListener('click', function () {
      if (radio.dataset.wasChecked === 'true') radio.checked = false;
    });
  });
})();

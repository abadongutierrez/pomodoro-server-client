const site = new URLSearchParams(location.search).get('site') || 'this site'
document.getElementById('site-name').textContent = site

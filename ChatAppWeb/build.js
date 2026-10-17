const fs = require('fs');
const html = fs.readFileSync('index.html', 'utf8');
const css = fs.readFileSync('style.css', 'utf8');
const js = fs.readFileSync('script.js', 'utf8');
let combined = html.replace('<link rel="stylesheet" href="style.css">', '<style>' + css + '</style>');
combined = combined.replace('<script src="script.js"></script>', '<script>' + js + '</script>');
fs.writeFileSync('SinglePageApp.html', combined);
console.log('Combined SPA created successfully at SinglePageApp.html');

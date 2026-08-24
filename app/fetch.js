const https = require('https');
https.get('https://developers.google.com/ml-kit/release-notes', (res) => {
  let data = '';
  res.on('data', (chunk) => { data += chunk; });
  res.on('end', () => {
    const lines = data.split('\n');
    lines.forEach(line => {
      if (line.toLowerCase().includes('cyrillic') || line.toLowerCase().includes('text-recognition')) {
        console.log(line.trim());
      }
    });
  });
}).on("error", (err) => {
  console.log("Error: " + err.message);
});

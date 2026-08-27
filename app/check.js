const https = require('https');
const versions = ['16.0.0', '16.0.1', '16.1.0', '16.2.0', '16.0.0-beta1', '16.0.0-beta2'];
versions.forEach(v => {
  https.get(`https://dl.google.com/dl/android/maven2/com/google/mlkit/text-recognition-cyrillic/${v}/text-recognition-cyrillic-${v}.pom`, (res) => {
    console.log(`Version ${v}: ${res.statusCode}`);
  });
});

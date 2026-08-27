import fs from 'fs';
import https from 'https';
import path from 'path';

const dir = 'app/src/main/assets/tessdata';
fs.mkdirSync(dir, { recursive: true });

function download(url, dest) {
    return new Promise((resolve, reject) => {
        const file = fs.createWriteStream(dest);
        https.get(url, (response) => {
            if (response.statusCode === 302) {
                download(response.headers.location, dest).then(resolve).catch(reject);
                return;
            }
            response.pipe(file);
            file.on('finish', () => {
                file.close(resolve);
            });
        }).on('error', (err) => {
            fs.unlink(dest, () => {});
            reject(err);
        });
    });
}

async function main() {
    await download('https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata', path.join(dir, 'eng.traineddata'));
    await download('https://github.com/tesseract-ocr/tessdata_fast/raw/main/rus.traineddata', path.join(dir, 'rus.traineddata'));
    console.log('Downloaded tessdata successfully.');
}

main().catch(console.error);

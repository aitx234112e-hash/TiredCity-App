const fs = require('fs');
const path = require('path');

async function run() {
  try {
    const distIndex = path.join(process.cwd(), 'dist', 'my-app', 'browser');
    const src = path.join(distIndex, 'index.csr.html');
    const dest = path.join(distIndex, 'index.html');

    if (!fs.existsSync(src)) {
      console.log('postbuild: index.csr.html not found, nothing to rename.');
      return;
    }

    await fs.promises.copyFile(src, dest);
    console.log('postbuild: copied index.csr.html -> index.html');
  } catch (err) {
    console.error('postbuild error:', err);
    process.exitCode = 1;
  }
}

run();

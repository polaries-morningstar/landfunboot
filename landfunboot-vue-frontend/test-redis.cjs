const Redis = require('ioredis');
const redis = new Redis('redis://:yingjun1218@175.24.206.249:6379');

async function main() {
    const keys = await redis.keys('user:permissions:*');
    console.log("Keys:", keys);
    if(keys.length > 0) {
        for(let k of keys) {
            const mems = await redis.smembers(k);
            console.log(k, mems);
        }
    }
    process.exit(0);
}
main();

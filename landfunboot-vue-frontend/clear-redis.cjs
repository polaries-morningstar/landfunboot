const Redis = require('ioredis');
const redis = new Redis('redis://:yingjun1218@175.24.206.249:6379');

async function main() {
    const keys = await redis.keys('user:*');
    if (keys.length > 0) {
        await redis.del(...keys);
        console.log("Cleared keys: ", keys.length);
    } else {
        console.log("No keys found.");
    }
    process.exit(0);
}
main();

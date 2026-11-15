const API_URL = process.env.SEED_API_URL ?? 'http://localhost:8080';
const PASSWORD = 'Rendezvous123';
const TICKS = 15;
const TICK_MS = 1000;
const HOUR_MS = 60 * 60 * 1000;

const MARKET_SQUARE = { lat: 50.0617, lon: 19.9372, radiusM: 300 };

const PEOPLE = {
  tester: { email: 'test@rendezvous.local', displayName: 'Tester' },
  anna: { email: 'anna@rendezvous.local', displayName: 'Anna Kowalska' },
  bartek: { email: 'bartek@rendezvous.local', displayName: 'Bartek Nowak' },
  celina: { email: 'celina@rendezvous.local', displayName: 'Celina Wiśniewska' },
  dawid: { email: 'dawid@rendezvous.local', displayName: 'Dawid Zieliński' },
  ewa: { email: 'ewa@rendezvous.local', displayName: 'Ewa Lewandowska' },
  franek: { email: 'franek@rendezvous.local', displayName: 'Franek Wójcik' },
};

const ROUTES = {
  tester: [[50.0617, 19.937], [50.0612, 19.938], [50.0608, 19.9372]],
  anna: [[50.0622, 19.9365], [50.063, 19.9378], [50.0625, 19.939]],
  bartek: [[50.0605, 19.936], [50.0598, 19.9352]],
  celina: [[50.0614, 19.9385], [50.0619, 19.9395]],
  dawid: [[50.0617, 19.9372], [50.064, 19.94], [50.066, 19.943]],
  ewa: [[50.061, 19.9365], [50.0585, 19.934], [50.0612, 19.9368]],
};

class ApiError extends Error {
  constructor(method, path, status, body) {
    super(`${method} ${path} → ${status} ${body}`);
    this.status = status;
    this.body = body;
  }
}

async function call(method, path, { token, body } = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  if (!response.ok) {
    throw new ApiError(method, path, response.status, text);
  }
  return text ? JSON.parse(text) : null;
}

const inHours = (hours) => new Date(Date.now() + hours * HOUR_MS).toISOString();
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function register({ email, displayName }) {
  const auth = await call('POST', '/api/auth/register', { body: { email, password: PASSWORD, displayName } });
  return { id: auth.user.id, token: auth.accessToken };
}

async function createRoom(owner, request, settings) {
  const room = await call('POST', '/api/rooms', { token: owner.token, body: request });
  if (!settings) {
    return room;
  }
  return call('PATCH', `/api/rooms/${room.id}`, { token: owner.token, body: { version: room.version, ...settings } });
}

const join = (user, room) => call('POST', '/api/rooms/join', { token: user.token, body: { code: room.code } });

function positionAt(route, tick) {
  const progress = (tick / (TICKS - 1)) * (route.length - 1);
  const segment = Math.min(Math.floor(progress), route.length - 2);
  const t = progress - segment;
  const [lat1, lon1] = route[segment];
  const [lat2, lon2] = route[segment + 1];
  return { lat: lat1 + (lat2 - lat1) * t, lon: lon1 + (lon2 - lon1) * t };
}

async function walk(users) {
  for (let tick = 0; tick < TICKS; tick += 1) {
    const recordedAt = new Date().toISOString();
    await Promise.all(
      Object.entries(ROUTES).map(([name, route]) =>
        call('POST', '/api/me/locations', {
          token: users[name].token,
          body: { points: [{ ...positionAt(route, tick), accuracyM: 10, recordedAt }] },
        }),
      ),
    );
    process.stdout.write(`\rRuch uczestników: ${tick + 1}/${TICKS}`);
    await sleep(TICK_MS);
  }
  process.stdout.write('\n');
}

async function blockAsAdmin(user) {
  const { APP_ADMIN_EMAIL: email, APP_ADMIN_PASSWORD: password } = process.env;
  if (!email || !password) {
    console.warn('Pominięto blokadę: brak APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD (uruchom z --env-file=.env).');
    return false;
  }
  const admin = await call('POST', '/api/auth/login', { body: { email, password } });
  await call('PUT', `/api/admin/users/${user.id}/block`, { token: admin.accessToken });
  return true;
}

async function registerEveryone() {
  const users = {};
  for (const [name, person] of Object.entries(PEOPLE)) {
    users[name] = await register(person);
  }
  return users;
}

async function main() {
  let users;
  try {
    users = await registerEveryone();
  } catch (error) {
    if (error instanceof ApiError && error.status === 409) {
      console.error('Dane demo już są w bazie (adres e-mail zajęty). Skrypt uruchamia się na pustej bazie.');
      process.exitCode = 1;
      return;
    }
    throw error;
  }
  const { tester, anna, bartek, celina, dawid, ewa, franek } = users;

  const walkRoom = await createRoom(tester, { name: 'Spacer po Starym Mieście', endsAt: inHours(48) }, {
    membersSeeTrails: true,
  });
  for (const user of [anna, bartek, celina, dawid, ewa]) {
    await join(user, walkRoom);
  }
  await call('PATCH', `/api/rooms/${walkRoom.id}/members/${anna.id}`, { token: tester.token, body: { role: 'ADMIN' } });
  await call('PUT', `/api/rooms/${walkRoom.id}/geofence`, { token: tester.token, body: MARKET_SQUARE });

  const familyRoom = await createRoom(anna, { name: 'Rodzinne wyjście na Planty', endsAt: inHours(6) }, {
    visibilityMode: 'GUARDIANS_ONLY',
  });
  await join(tester, familyRoom);
  await join(bartek, familyRoom);

  const matchRoom = await createRoom(tester, { name: 'Mecz w Tauron Arenie', startsAt: inHours(24), endsAt: inHours(27) });
  await join(ewa, matchRoom);

  await walk(users);

  await call('POST', `/api/rooms/${walkRoom.id}/leave`, { token: celina.token });

  const bbqRoom = await createRoom(tester, { name: 'Grill nad Wisłą', endsAt: inHours(3) });
  await join(bartek, bbqRoom);
  await call('POST', `/api/rooms/${bbqRoom.id}/close`, { token: tester.token });

  await createRoom(franek, { name: 'Nocny bieg po Błoniach', endsAt: inHours(5) });
  const blocked = await blockAsAdmin(franek);

  console.log('\nGotowe. Konta (hasło dla wszystkich: %s):', PASSWORD);
  console.table(
    Object.entries(PEOPLE).map(([name, person]) => ({
      email: person.email,
      nazwa: person.displayName,
      uwagi: name === 'tester' ? 'główne konto testowe' : name === 'franek' && blocked ? 'zablokowany' : '',
    })),
  );
  console.log('Pokoje: „%s” (kod %s), „%s” (kod %s), „%s” (zaplanowany), „%s” (zakończony).',
    walkRoom.name, walkRoom.code, familyRoom.name, familyRoom.code, matchRoom.name, bbqRoom.name);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

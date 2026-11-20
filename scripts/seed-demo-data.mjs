const API_URL = process.env.SEED_API_URL ?? 'http://localhost:8080';
const PASSWORD = 'Rendezvous123';
const TICKS = 60;
const TICK_MS = 1000;
const POINTS_PER_TICK = 4;
const JITTER_DEG = 0.00003;
const HOUR_MS = 60 * 60 * 1000;

const PEOPLE = {
  tester: { email: 'test@rendezvous.local', displayName: 'Tester' },
  anna: { email: 'anna@rendezvous.local', displayName: 'Anna Kowalska' },
  bartek: { email: 'bartek@rendezvous.local', displayName: 'Bartek Nowak' },
  celina: { email: 'celina@rendezvous.local', displayName: 'Celina Wiśniewska' },
  dawid: { email: 'dawid@rendezvous.local', displayName: 'Dawid Zieliński' },
  ewa: { email: 'ewa@rendezvous.local', displayName: 'Ewa Lewandowska' },
  magda: { email: 'magda@rendezvous.local', displayName: 'Magda Kamińska' },
  janek: { email: 'janek@rendezvous.local', displayName: 'Janek Wróbel' },
  kasia: { email: 'kasia@rendezvous.local', displayName: 'Kasia Mazur' },
  tomek: { email: 'tomek@rendezvous.local', displayName: 'Tomek Krawczyk' },
  ola: { email: 'ola@rendezvous.local', displayName: 'Ola Piotrowska' },
  piotr: { email: 'piotr@rendezvous.local', displayName: 'Piotr Grabowski' },
  zosia: { email: 'zosia@rendezvous.local', displayName: 'Zosia Pawlak' },
  kuba: { email: 'kuba@rendezvous.local', displayName: 'Kuba Michalski' },
  lena: { email: 'lena@rendezvous.local', displayName: 'Lena Król' },
  iga: { email: 'iga@rendezvous.local', displayName: 'Iga Wieczorek' },
  olek: { email: 'olek@rendezvous.local', displayName: 'Olek Jabłoński' },
  franek: { email: 'franek@rendezvous.local', displayName: 'Franek Wójcik' },
};

const ZONES = {
  marketSquare: { lat: 50.0617, lon: 19.9372, radiusM: 300 },
  wawel: { lat: 50.054, lon: 19.9354, radiusM: 250 },
  saltMine: { lat: 49.9835, lon: 20.0555, radiusM: 200 },
};

const ROUTES = {
  tester: [[50.0619, 19.9366], [50.0612, 19.9382], [50.0606, 19.9378], [50.059, 19.9379], [50.0575, 19.9381],
    [50.056, 19.9376], [50.0549, 19.9362], [50.0543, 19.9355]],
  anna: [[50.0623, 19.9368], [50.0628, 19.938], [50.0631, 19.9392], [50.0624, 19.9396], [50.0616, 19.9394],
    [50.0612, 19.9385], [50.0618, 19.9374]],
  bartek: [[50.0611, 19.9362], [50.0604, 19.9355], [50.0598, 19.9349], [50.0603, 19.9343], [50.0612, 19.935],
    [50.0618, 19.9358]],
  celina: [[50.0614, 19.9385], [50.062, 19.9392], [50.0626, 19.9387], [50.0622, 19.9378], [50.0616, 19.9381]],
  dawid: [[50.0618, 19.9374], [50.0626, 19.9384], [50.0636, 19.9396], [50.0645, 19.9407], [50.0652, 19.9414],
    [50.0656, 19.9418]],
  ewa: [[50.061, 19.9368], [50.0597, 19.9376], [50.0582, 19.938], [50.057, 19.9382], [50.0585, 19.9379],
    [50.06, 19.9376], [50.0611, 19.937]],
  magda: [[50.0548, 19.9361], [50.0543, 19.9356], [50.0538, 19.9352], [50.0541, 19.9347], [50.0546, 19.935]],
  janek: [[50.0536, 19.936], [50.0532, 19.9356], [50.0535, 19.9349], [50.054, 19.9345]],
  kasia: [[50.0545, 19.9358], [50.054, 19.9352], [50.0534, 19.935], [50.0537, 19.9344], [50.0543, 19.9348]],
  tomek: [[50.0542, 19.9356], [50.0535, 19.9345], [50.0527, 19.9335], [50.0518, 19.9325], [50.0512, 19.9318],
    [50.0525, 19.9332], [50.0537, 19.9348]],
  ola: [[50.0541, 19.9363], [50.0536, 19.9358], [50.0533, 19.9353], [50.0538, 19.9357]],
  piotr: [[50.0539, 19.935], [50.0544, 19.9345], [50.0548, 19.9351], [50.0543, 19.9357]],
  zosia: [[50.0516, 19.9447], [50.052, 19.9458], [50.0525, 19.9468], [50.053, 19.9476]],
  kuba: [[50.0509, 19.944], [50.0514, 19.945], [50.0519, 19.9461], [50.0526, 19.9471]],
  lena: [[50.0522, 19.9442], [50.0518, 19.9452], [50.0523, 19.9463], [50.0528, 19.9473]],
  iga: [[49.9836, 20.0553], [49.9833, 20.056], [49.9829, 20.0556], [49.9832, 20.0549]],
  olek: [[49.9834, 20.0556], [49.9842, 20.0566], [49.9852, 20.0578], [49.9856, 20.0583]],
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

async function joinAll(room, users) {
  for (const user of users) {
    await call('POST', '/api/rooms/join', { token: user.token, body: { code: room.code } });
  }
}

const promote = (owner, room, user) =>
  call('PATCH', `/api/rooms/${room.id}/members/${user.id}`, { token: owner.token, body: { role: 'ADMIN' } });

const setZone = (owner, room, zone) =>
  call('PUT', `/api/rooms/${room.id}/geofence`, { token: owner.token, body: zone });

function positionAt(route, progress) {
  const scaled = Math.min(progress, 1) * (route.length - 1);
  const segment = Math.min(Math.floor(scaled), route.length - 2);
  const t = scaled - segment;
  const [lat1, lon1] = route[segment];
  const [lat2, lon2] = route[segment + 1];
  return { lat: lat1 + (lat2 - lat1) * t, lon: lon1 + (lon2 - lon1) * t };
}

const jitter = ({ lat, lon }) => ({
  lat: lat + (Math.random() - 0.5) * JITTER_DEG,
  lon: lon + (Math.random() - 0.5) * JITTER_DEG * 1.5,
});

function tickPoints(route, tick, from, to) {
  return Array.from({ length: POINTS_PER_TICK }, (_, i) => {
    const step = (i + 1) / POINTS_PER_TICK;
    return {
      ...jitter(positionAt(route, (tick - 1 + step) / TICKS)),
      accuracyM: 5 + Math.round(Math.random() * 10),
      recordedAt: new Date(from + (to - from) * step).toISOString(),
    };
  });
}

async function walk(users) {
  let previous = Date.now();
  for (let tick = 1; tick <= TICKS; tick += 1) {
    await sleep(TICK_MS);
    const now = Date.now();
    await Promise.all(
      Object.entries(ROUTES).map(([name, route]) =>
        call('POST', '/api/me/locations', {
          token: users[name].token,
          body: { points: tickPoints(route, tick, previous, now) },
        }),
      ),
    );
    previous = now;
    process.stdout.write(`\rRuch uczestników: ${tick}/${TICKS}`);
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
  const u = users;

  const walkRoom = await createRoom(u.tester, { name: 'Spacer po Starym Mieście', endsAt: inHours(48) }, {
    membersSeeTrails: true,
  });
  await joinAll(walkRoom, [u.anna, u.bartek, u.celina, u.dawid, u.ewa]);
  await promote(u.tester, walkRoom, u.anna);
  await setZone(u.tester, walkRoom, ZONES.marketSquare);

  const tripRoom = await createRoom(u.magda, { name: 'Wycieczka klasowa na Wawel', endsAt: inHours(8) }, {
    visibilityMode: 'GUARDIANS_ONLY',
  });
  await joinAll(tripRoom, [u.janek, u.kasia, u.tomek, u.ola, u.piotr, u.tester]);
  await promote(u.magda, tripRoom, u.janek);
  await setZone(u.magda, tripRoom, ZONES.wawel);

  const friendsRoom = await createRoom(u.zosia, { name: 'Wieczór na Kazimierzu', endsAt: inHours(6) }, {
    membersSeeTrails: true,
  });
  await joinAll(friendsRoom, [u.kuba, u.lena, u.tester]);

  const mineRoom = await createRoom(u.tester, { name: 'Wycieczka do Wieliczki', endsAt: inHours(5) });
  await joinAll(mineRoom, [u.iga, u.olek]);
  await setZone(u.tester, mineRoom, ZONES.saltMine);

  const matchRoom = await createRoom(u.tester, { name: 'Mecz w Tauron Arenie', startsAt: inHours(24), endsAt: inHours(27) });
  await joinAll(matchRoom, [u.ewa, u.bartek]);

  const mountainsRoom = await createRoom(u.anna, {
    name: 'Weekend w Zakopanem',
    startsAt: inHours(24 * 7),
    endsAt: inHours(24 * 7 + 48),
  });
  await joinAll(mountainsRoom, [u.tester, u.celina]);

  await walk(users);

  await call('POST', `/api/rooms/${walkRoom.id}/leave`, { token: u.celina.token });
  await call('POST', `/api/rooms/${mineRoom.id}/close`, { token: u.tester.token });

  const bbqRoom = await createRoom(u.tester, { name: 'Grill nad Wisłą', endsAt: inHours(3) });
  await joinAll(bbqRoom, [u.bartek, u.dawid]);
  await call('POST', `/api/rooms/${bbqRoom.id}/close`, { token: u.tester.token });

  await createRoom(u.franek, { name: 'Nocny bieg po Błoniach', endsAt: inHours(5) });
  const blocked = await blockAsAdmin(u.franek);

  console.log('\nGotowe. Konta (hasło dla wszystkich: %s):', PASSWORD);
  console.table(
    Object.entries(PEOPLE).map(([name, person]) => ({
      email: person.email,
      nazwa: person.displayName,
      uwagi: name === 'tester' ? 'główne konto testowe' : name === 'franek' && blocked ? 'zablokowany' : '',
    })),
  );
  console.table([
    { pokój: walkRoom.name, kod: walkRoom.code, stan: 'aktywny, strefa na Rynku, trasy widoczne' },
    { pokój: tripRoom.name, kod: tripRoom.code, stan: 'aktywny, tylko opiekunowie, strefa Wawel' },
    { pokój: friendsRoom.name, kod: friendsRoom.code, stan: 'aktywny, trasy widoczne' },
    { pokój: matchRoom.name, kod: matchRoom.code, stan: 'zaplanowany (jutro)' },
    { pokój: mountainsRoom.name, kod: mountainsRoom.code, stan: 'zaplanowany (za tydzień)' },
    { pokój: mineRoom.name, kod: mineRoom.code, stan: 'zakończony, z historią alertów' },
    { pokój: bbqRoom.name, kod: bbqRoom.code, stan: 'zakończony' },
  ]);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});

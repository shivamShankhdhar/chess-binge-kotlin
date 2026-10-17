const fs = require('fs');
const WebSocket = require('/Users/shivamshankhdhar/Projects/attendance-tracking-mobile-app/node_modules/ws');
const http = require('http');

async function getPageWsUrl() {
  return new Promise((resolve, reject) => {
    http.get('http://localhost:9222/json/list', (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        const list = JSON.parse(data);
        const page = list.find(p => p.url.includes('localhost:8082'));
        if (page) resolve(page.webSocketDebuggerUrl);
        else reject(new Error('Page not found'));
      });
    }).on('error', reject);
  });
}

async function run() {
  const wsUrl = await getPageWsUrl();
  console.log('Connecting to:', wsUrl);
  const ws = new WebSocket(wsUrl);

  let idCounter = 1;
  const pending = new Map();

  ws.on('message', (msg) => {
    const data = JSON.parse(msg);
    if (data.id && pending.has(data.id)) {
      const { resolve, reject } = pending.get(data.id);
      pending.delete(data.id);
      if (data.error) reject(data.error);
      else resolve(data.result);
    }
  });

  await new Promise(resolve => ws.on('open', resolve));
  console.log('WS connected!');

  function send(method, params = {}) {
    return new Promise((resolve, reject) => {
      const id = idCounter++;
      pending.set(id, { resolve, reject });
      ws.send(JSON.stringify({ id, method, params }));
    });
  }

  async function evaluate(expression) {
    const res = await send('Runtime.evaluate', {
      expression,
      returnByValue: true,
      awaitPromise: true,
    });
    return res.result ? res.result.value : null;
  }

  async function screenshot(filename) {
    const res = await send('Page.captureScreenshot', { format: 'png' });
    const buffer = Buffer.from(res.data, 'base64');
    fs.writeFileSync(filename, buffer);
    console.log(`Saved screenshot to ${filename} (${buffer.length} bytes)`);
  }

  // Set device emulation
  await send('Emulation.setDeviceMetricsOverride', {
    width: 412,
    height: 915,
    deviceScaleFactor: 2.625,
    mobile: true,
  });

  // Enable Page & Runtime
  await send('Page.enable');
  await send('Runtime.enable');

  // Let's see what's on the page right now
  const title = await evaluate('document.title');
  const bodyText = await evaluate('document.body.innerText');
  console.log('Title:', title);
  console.log('Body snippet:', bodyText.slice(0, 300));

  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_0_welcome.png');

  // Click Reviewer Login if present
  console.log('Clicking Reviewer Login...');
  await evaluate(`(() => {
    const elements = Array.from(document.querySelectorAll('*'));
    const btn = elements.find(el => el.innerText && el.innerText.trim() === 'Reviewer Login');
    if (btn) {
      btn.click();
      return 'clicked Reviewer Login';
    }
    return 'Reviewer Login not found';
  })()`);

  await new Promise(r => setTimeout(r, 1500));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_1_reviewer.png');

  // Click the first "Log in" button
  console.log('Clicking Log in button for Employer Account...');
  await evaluate(`(() => {
    const btns = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim() === 'Log in');
    if (btns.length > 0) {
      btns[0].click();
      // Also try parent click in case
      if (btns[0].parentElement) btns[0].parentElement.click();
      return 'clicked Log in button';
    }
    return 'Log in not found';
  })()`);

  // Dismiss MPIN setup if shown
  console.log('Checking for Set up later button...');
  await evaluate(`(() => {
    const btns = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim() === 'Set up later');
    if (btns.length > 0) {
      btns[0].click();
      if (btns[0].parentElement) btns[0].parentElement.click();
      return 'clicked Set up later';
    }
    return 'Set up later not found';
  })()`);

  await new Promise(r => setTimeout(r, 2000));
  const bodyAfterMpin = await evaluate('document.body.innerText');
  console.log('Body on dashboard:', bodyAfterMpin.slice(0, 300));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_2_today.png');

  // Let's capture Attendance tab
  console.log('Navigating to Attendance tab...');
  await evaluate(`(() => {
    const tabs = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim() === 'Attendance');
    if (tabs.length > 0) {
      const tab = tabs[tabs.length - 1];
      tab.click();
      if (tab.parentElement) tab.parentElement.click();
      return 'clicked Attendance';
    }
    return 'Attendance tab not found';
  })()`);

  await new Promise(r => setTimeout(r, 2000));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_3_attendance_mark.png');

  // Attendance -> Requests subtab
  console.log('Navigating to Attendance Requests subtab...');
  await evaluate(`(() => {
    const tabs = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim().startsWith('Requests'));
    if (tabs.length > 0) {
      tabs[0].click();
      if (tabs[0].parentElement) tabs[0].parentElement.click();
      return 'clicked Requests';
    }
    return 'Requests not found';
  })()`);
  await new Promise(r => setTimeout(r, 2000));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_4_attendance_requests.png');

  // Attendance -> Daily subtab
  console.log('Navigating to Attendance Daily subtab...');
  await evaluate(`(() => {
    const tabs = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim() === 'Daily');
    if (tabs.length > 0) {
      tabs[0].click();
      if (tabs[0].parentElement) tabs[0].parentElement.click();
      return 'clicked Daily';
    }
    return 'Daily not found';
  })()`);
  await new Promise(r => setTimeout(r, 2000));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_5_attendance_daily.png');

  // Attendance -> Reports subtab
  console.log('Navigating to Attendance Reports subtab...');
  await evaluate(`(() => {
    const tabs = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim() === 'Reports');
    if (tabs.length > 0) {
      tabs[0].click();
      if (tabs[0].parentElement) tabs[0].parentElement.click();
      return 'clicked Reports';
    }
    return 'Reports not found';
  })()`);
  await new Promise(r => setTimeout(r, 2000));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_6_attendance_reports.png');

  // Employees tab
  console.log('Navigating to Employees tab...');
  await evaluate(`(() => {
    const tabs = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim() === 'Employees');
    if (tabs.length > 0) {
      const tab = tabs[tabs.length - 1]; // bottom bar
      tab.click();
      if (tab.parentElement) tab.parentElement.click();
      return 'clicked Employees';
    }
    return 'Employees tab not found';
  })()`);
  await new Promise(r => setTimeout(r, 2000));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_7_employees_all.png');

  // Workplace tab
  console.log('Navigating to Workplace tab...');
  await evaluate(`(() => {
    const tabs = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim() === 'Workplace');
    if (tabs.length > 0) {
      const tab = tabs[tabs.length - 1]; // bottom bar
      tab.click();
      if (tab.parentElement) tab.parentElement.click();
      return 'clicked Workplace';
    }
    return 'Workplace tab not found';
  })()`);
  await new Promise(r => setTimeout(r, 2000));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_8_workplace.png');

  // Workplace -> Invite subtab
  console.log('Navigating to Workplace Invite subtab...');
  await evaluate(`(() => {
    const tabs = Array.from(document.querySelectorAll('*')).filter(el => el.innerText && el.innerText.trim() === 'Invite');
    if (tabs.length > 0) {
      tabs[0].click();
      if (tabs[0].parentElement) tabs[0].parentElement.click();
      return 'clicked Workplace Invite';
    }
    return 'Workplace Invite not found';
  })()`);
  await new Promise(r => setTimeout(r, 2000));
  await screenshot('/Users/shivamshankhdhar/Projects/rn_screen_9_workplace_invite.png');

  ws.close();
}

run().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});

const $ = s => document.querySelector(s);
const $$ = s => document.querySelectorAll(s);

let data = {};

let session = JSON.parse(
    localStorage.getItem('resqsimSession') || 'null'
);


/* =====================================================
   API
===================================================== */

async function api(url, opt = {}) {

    const r = await fetch('/api' + url, {

        headers: {
            'Content-Type': 'application/json',
            ...(opt.headers || {})
        },

        ...opt
    });

    let body = {};

    try {
        body = await r.json();
    } catch (_) {}

    if (!r.ok) {

        throw Error(
            body.message || 'Request failed'
        );
    }

    return body;
}


/* =====================================================
   TOAST
===================================================== */

function toast(message) {

    const x = $('#toast');

    if (!x) {
        alert(message);
        return;
    }

    x.textContent = message;
    x.style.display = 'block';

    setTimeout(() => {
        x.style.display = 'none';
    }, 2800);
}


/* =====================================================
   MODALS
===================================================== */

function openModal(id) {

    const element = $('#' + id);

    if (element) {
        element.classList.add('show');
    }
}


function closeModal(id) {

    const element = $('#' + id);

    if (element) {
        element.classList.remove('show');
    }
}


/* =====================================================
   LOGIN TABS
===================================================== */

$$('.login-tab').forEach(button => {

    button.onclick = () => {

        $$('.login-tab').forEach(x => {
            x.classList.remove('active');
        });

        button.classList.add('active');

        const admin =
            button.dataset.role === 'admin';

        if ($('#adminLogin')) {

            $('#adminLogin')
                .classList
                .toggle('hidden', !admin);
        }

        if ($('#victimLogin')) {

            $('#victimLogin')
                .classList
                .toggle('hidden', admin);
        }
    };

});


/* =====================================================
   ADMIN LOGIN
===================================================== */

const adminLoginForm = $('#adminLogin');

if (adminLoginForm) {

    adminLoginForm.onsubmit = async e => {

        e.preventDefault();

        const form = new FormData(e.target);

        await doLogin({

            role: 'admin',

            email:
                form.get('email'),

            password:
                form.get('password')

        });
    };
}


/* =====================================================
   VICTIM LOGIN
===================================================== */

const victimLoginForm = $('#victimLogin');

if (victimLoginForm) {

    victimLoginForm.onsubmit = async e => {

        e.preventDefault();

        const form = new FormData(e.target);

        const phone =
            form.get('phone');

        await doLogin({

            role: 'victim',

            victimId:
                Number(
                    form.get('victimId')
                ),

            phone: phone,

            email: phone

        });
    };
}


/* =====================================================
   LOGIN
===================================================== */

async function doLogin(payload) {

    try {

        const response =
            await api(
                '/login',
                {
                    method: 'POST',
                    body:
                        JSON.stringify(payload)
                }
            );

        session = response;

        localStorage.setItem(
            'resqsimSession',
            JSON.stringify(response)
        );

        showApp();

        toast(
            'Signed in successfully'
        );

    } catch (error) {

        toast(error.message);

        console.error(
            'Login error:',
            error
        );
    }
}


/* =====================================================
   SHOW APPLICATION
===================================================== */
function showApp() {
    if (!session) return;

    const loginScreen = $('#loginScreen');

    if (loginScreen) {
        loginScreen.classList.add('hidden');
    }

    if (session.role === 'admin') {
        stopVictimAutoRefresh();

        if ($('#adminApp')) {
            $('#adminApp').classList.remove('hidden');
        }

        if ($('#victimApp')) {
            $('#victimApp').classList.add('hidden');
        }

        loadAll();
    } else {
        if ($('#victimApp')) {
            $('#victimApp').classList.remove('hidden');
        }

        if ($('#adminApp')) {
            $('#adminApp').classList.add('hidden');
        }

        loadVictimPortal();
        startVictimAutoRefresh();
    }
}

/* =====================================================
   LOGOUT
===================================================== */

function logout() {
    stopVictimAutoRefresh();

    session = null;

    localStorage.removeItem('resqsimSession');

    if ($('#adminApp')) {
        $('#adminApp').classList.add('hidden');
    }

    if ($('#victimApp')) {
        $('#victimApp').classList.add('hidden');
    }

    if ($('#loginScreen')) {
        $('#loginScreen').classList.remove('hidden');
    }
}

/* =====================================================
   PAGE LABELS
===================================================== */

const labels = {

    dashboard: [
        'Operations Dashboard',
        'Live disaster response overview'
    ],

    victims: [
        'Victim Management',
        'Registered people and current response status'
    ],

    sos: [
        'Emergency / SOS Queue',
        'Prioritize and dispatch help'
    ],

    teams: [
        'Rescue Teams',
        'Dispatch teams to active emergencies'
    ],

    volunteers: [
        'Volunteers',
        'People and skills supporting response'
    ],

    shelters: [
        'Shelters',
        'Emergency accommodation capacity'
    ],

    resources: [
        'Resources',
        'Supply requests and allocation'
    ],

    scheduling: [
        'OS Scheduling Lab',
        'FCFS and Priority scheduling'
    ],

    reports: [
        'Response Report',
        'Operational summary'
    ]
};


/* =====================================================
   NAVIGATION
===================================================== */

function go(page) {

    $$('.page').forEach(x => {
        x.classList.remove('active');
    });

    const target =
        $('#' + page);

    if (target) {
        target.classList.add('active');
    }

    $$('.nav').forEach(x => {

        x.classList.toggle(
            'active',
            x.dataset.page === page
        );

    });

    if (labels[page]) {

        if ($('#pageTitle')) {

            $('#pageTitle').textContent =
                labels[page][0];
        }

        if ($('#pageSub')) {

            $('#pageSub').textContent =
                labels[page][1];
        }
    }


    if (page === 'reports') {
        loadReport();
    }


    if (page === 'teams') {
    loadRescueRequests();
}

}


$$('.nav').forEach(button => {

    button.onclick = () => {

        go(
            button.dataset.page
        );

    };

});


/* =====================================================
   LOAD ADMIN DATA
===================================================== */

async function loadAll() {

    try {

        const [
            dash,
            victims,
            sos,
            teams,
            rescueRequests,
            vols,
            shelters,
            resources,
            inventory
        ] = await Promise.all([

            api('/dashboard'),

            api('/victims'),

            api('/admin/sos'),

            api('/teams'),

            api('/teams/rescue-requests'),

            api('/volunteers'),

            api('/shelters'),

            api('/admin/resource-requests'),

            api('/admin/inventory')

        ]);


        data = {

            victims:
                victims || [],

            teams:
                teams || [],

            rescueRequests:
                rescueRequests || [],

            sos:
                sos || [],

            vols:
                vols || [],

            shelters:
                shelters || [],

            res:
                resources || [],

            inventory:
                inventory || []

        };


        renderDash(dash);

        renderVictims(
            data.victims
        );

        renderSos(
            data.sos
        );

        renderTeams(
            data.teams,
            data.rescueRequests
        );

        renderVols(
            data.vols
        );

        renderShelters(
            data.shelters
        );

        renderResources(
            data.res
        );

        renderInventory(
            data.inventory
        );

        fillSelects(
            data.shelters,
            data.teams
        );


        const health =
            await api('/health');


        const databaseOK =
            health.success === true ||
            health.ok === true;


        if ($('#dbState')) {

            $('#dbState').textContent =
                databaseOK
                    ? '● MySQL connected'
                    : '● Database error';

            $('#dbState').className =
                'db-pill ' +
                (
                    databaseOK
                        ? 'ok'
                        : 'bad'
                );
        }


    } catch (error) {

        console.error(
            'Load error:',
            error
        );

        if ($('#dbState')) {

            $('#dbState').textContent =
                '● Database offline';

            $('#dbState').className =
                'db-pill bad';
        }

        toast(
            'Could not load data: ' +
            error.message
        );
    }
}


/* =====================================================
   DASHBOARD
===================================================== */

function renderDash(d = {}) {

    const pendingSOS =
        d.pendingSOS ??
        d.pendingSos ??
        0;


    const availableBeds =
        d.shelterBeds ??
        d.availableBeds ??
        0;


    const availableTeams =
        d.availableTeams ??
        0;


    const activeTeams =
        d.activeTeams ??
        0;


    const volunteers =
        d.volunteers ??
        0;


    if ($('#heroSos')) {

        $('#heroSos').textContent =
            pendingSOS;
    }


    if ($('#stats')) {

        $('#stats').innerHTML = [

            [
                'Registered victims',
                d.victims || 0,
                '👥'
            ],

            [
                'Pending SOS',
                pendingSOS,
                '🚨'
            ],

            [
                'Resolved SOS',
                d.resolvedSOS || 0,
                '✅'
            ],

            [
                'High-priority SOS',
                d.highPrioritySOS || 0,
                '⚠️'
            ],

            [
                'Assigned teams',
                activeTeams,
                '🚑'
            ],

            [
                'Available beds',
                availableBeds,
                '🏠'
            ]

        ].map(x => `

            <div class="stat">

                <span>${x[2]}</span>

                <small>${x[0]}</small>

                <b>${x[1]}</b>

            </div>

        `).join('');
    }


    if ($('#dashSos')) {

        $('#dashSos').innerHTML =

            (data.sos || [])

                .slice(0, 5)

                .map(x => `

                    <div class="alert-row">

                        <div>

                            <b>
                                ${escapeHtml(
                                    x.name
                                )}
                            </b>

                            <small>

                                ${escapeHtml(
                                    x.location ||
                                    'Location pending'
                                )}

                                ·

                                ${escapeHtml(
                                    x.emergency_type ||
                                    'Emergency'
                                )}

                            </small>

                        </div>

                        <span class="badge red">

                            Severity
                            ${x.severity}

                        </span>

                    </div>

                `).join('')

            ||

            '<p class="muted">No pending SOS requests.</p>';
    }


    const total =
        (
            availableTeams +
            activeTeams
        ) || 1;


    if ($('#capacity')) {

        $('#capacity').innerHTML = `

            <div class="metric">

                <div>

                    <span>
                        Teams available
                    </span>

                    <b>
                        ${availableTeams}
                    </b>

                </div>

                <div class="mini-progress">

                    <span
                        style="width:${
                            (
                                availableTeams /
                                total
                            ) * 100
                        }%">
                    </span>

                </div>

            </div>


            <div class="metric">

                <div>

                    <span>
                        Volunteers registered
                    </span>

                    <b>
                        ${volunteers}
                    </b>

                </div>

            </div>

        `;
    }
}


/* =====================================================
   BADGE
===================================================== */

function badge(status) {

    const value =
        String(status || '—');


    const className =

        value.includes('Pending') ||
        value.includes('Severity 5') ||
        value.includes('Priority 4')

            ? 'red'

            : value.includes('Allocated') ||
              value === 'Approved' ||
              value === 'Available' ||
              value === 'Completed'

                ? 'green'

                : 'blue';


    return `

        <span class="badge ${className}">

            ${escapeHtml(value)}

        </span>

    `;
}


/* =====================================================
   VICTIMS
===================================================== */

function renderVictims(a) {

    const rows =
        a || [];


    if (!$('#victimRows')) {
        return;
    }


    $('#victimRows').innerHTML =

        rows.map(x => `

            <tr>

                <td>
                    #${x.victim_id}
                </td>

                <td>
                    <b>
                        ${escapeHtml(
                            x.name
                        )}
                    </b>
                </td>

                <td>
                    ${escapeHtml(
                        x.phone || '—'
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        x.location || '—'
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        x.emergency_type || '—'
                    )}
                </td>

                <td>
                    ${x.severity || '—'}
                </td>

                <td>
                    ${badge(
                        x.status
                    )}
                </td>

                <td>

                    ${
                        x.team_id

                            ? `

                                <div class="team-cell">

                                    <b>
                                        #${x.team_id}
                                    </b>

                                    <span>
                                        ${escapeHtml(
                                            x.team_name ||
                                            'Rescue Team'
                                        )}
                                    </span>

                                </div>

                              `

                            : `

                                <span class="muted">
                                    Not Assigned
                                </span>

                              `
                    }

                </td>

                <td>

                    <button
                        class="ghost small"
                        onclick="openSos(
                            ${x.victim_id}
                        )">

                        SOS

                    </button>


                    <button
                        class="ghost small"
                        onclick="requestResource(
                            ${x.victim_id}
                        )">

                        Resource

                    </button>

                </td>

            </tr>

        `).join('')

        ||

        `

            <tr>

                <td colspan="9">
                    No victims yet.
                </td>

            </tr>

        `;
}


/* =====================================================
   SOS
===================================================== */

function renderSos(a) {

    if (!$('#sosRows')) {
        return;
    }


    $('#sosRows').innerHTML =

        (a || []).map(x => `

            <tr>

                <td>
                    #${x.victim_id}
                </td>

                <td>

                    <b>
                        ${escapeHtml(
                            x.name
                        )}
                    </b>

                    <small>
                        ${escapeHtml(
                            x.phone || ''
                        )}
                    </small>

                </td>

                <td>
                    ${escapeHtml(
                        x.location || '—'
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        x.emergency_type || '—'
                    )}
                </td>

                <td>

                    ${badge(
                        'Severity ' +
                        x.severity
                    )}

                </td>

                <td>

                    ${
                        x.team_id
                            ? badge('Assigned')
                            : badge('Pending')
                    }

                </td>

                <td>

                    <button
                        class="ghost small"
                        onclick="openTeam(
                            ${x.victim_id}
                        )">

                        Assign team

                    </button>

                    <button
                        class="primary small"
                        onclick="resolveSos(
                            ${x.victim_id}
                        )">

                        Mark resolved

                    </button>

                </td>

            </tr>

        `).join('')

        ||

        `

            <tr>

                <td colspan="7">
                    No pending SOS requests.
                </td>

            </tr>

        `;
}


/* =====================================================
   RESOLVE SOS
===================================================== */

async function resolveSos(victimId) {

    if (!confirm(
        `Mark SOS for Victim #${victimId} as resolved?`
    )) {
        return;
    }

    try {
        const response = await api(
            `/admin/sos/${victimId}/resolve`,
            { method: 'PUT' }
        );

        toast(response.message || 'SOS marked as resolved');
        await loadAll();
    } catch (error) {
        toast(error.message);
    }
}
/* =====================================================
   RESCUE TEAMS
===================================================== */


/* =====================================================
   RESCUE TEAMS
   Displays team details and assigned victim details.
   Pending rescue requests are NOT shown inside cards.
   ===================================================== */

function renderTeams(teams) {
    const container = $('#teamCards');

    if (!container) return;

    container.innerHTML = (teams || []).map(team => {
        const assignedVictim =
            team.victim_id !== null &&
            team.victim_id !== undefined &&
            team.victim_id !== '' &&
            Number(team.victim_id) > 0;

        const status = team.rescue_status || 'Available';

        return `
            <div class="card team">
                <div class="team-icon">🚑</div>

                <h3>
                    #${escapeHtml(String(team.team_id ?? '—'))}
                    · ${escapeHtml(team.team_name || 'Rescue Team')}
                </h3>

                <p>
                    <b>Leader:</b>
                    ${escapeHtml(team.leader || '—')}
                </p>

                <p>
                    <b>Vehicle:</b>
                    ${escapeHtml(team.vehicle || '—')}
                </p>

                <p>
                    <b>Status:</b> ${badge(status)}
                </p>

                <div style="
                    margin-top:12px;
                    padding:12px;
                    border-radius:8px;
                    background:rgba(0,0,0,0.04);
                ">
                    <strong>Assigned Victim</strong>

                    ${
                        assignedVictim
                            ? `
                                <p>
                                    <b>Victim ID:</b>
                                    #${escapeHtml(String(team.victim_id))}
                                </p>

                                <p>
                                    <b>Name:</b>
                                    ${escapeHtml(team.victim_name || 'Name unavailable')}
                                </p>

                                <p>
                                    <b>Severity:</b>
                                    ${escapeHtml(
                                        team.victim_severity != null
                                            ? String(team.victim_severity)
                                            : 'Not available'
                                    )}
                                </p>

                                <p>
                                    <b>Location:</b>
                                    ${escapeHtml(team.victim_location || 'Location unavailable')}
                                </p>
                            `
                            : `<p class="muted">No victim assigned.</p>`
                    }
                </div>

                ${
                    status === 'Available'
                        ? `
                            <button
                                class="ghost small"
                                style="margin-top:12px;width:100%;"
                                onclick="acceptRescueOperation(${Number(team.team_id)})">
                                ✓ Accept Rescue Operation
                            </button>
                        `
                        : ''
                }

                <button
                    class="ghost small"
                    style="margin-top:8px;width:100%;"
                    onclick="updateRescueStatus(${Number(team.team_id)})">
                    🔄 Update Rescue Status
                </button>

                ${
                    assignedVictim
                        ? `
                            <button
                                class="ghost small"
                                style="margin-top:8px;width:100%;"
                                onclick="accessVictimLocation(${Number(team.team_id)})">
                                📍 Access Victim Location
                            </button>
                        `
                        : ''
                }
            </div>
        `;
    }).join('') || `
        <div class="card">
            <p>No rescue teams registered.</p>
        </div>
    `;
}



/* =====================================================
   RESCUE FEATURE 1
   VIEW RESCUE REQUESTS
   OS: PRIORITY SCHEDULING
===================================================== */


/* =====================================================
   RESCUE FEATURE 1
   LOAD PENDING RESCUE REQUESTS
   Used by Accept Rescue Operation.
   Requests are not displayed inside team cards.
   ===================================================== */

async function loadRescueRequests() {
    try {
        const requests = await api('/teams/rescue-requests');

        data.rescueRequests = requests || [];

    } catch (error) {
        console.error('Rescue request error:', error);

        toast(
            'Could not load rescue requests: ' + error.message
        );
    }
}

/* =====================================================
   RESCUE FEATURE 2
   ACCEPT RESCUE OPERATION
   OS: PROCESS SYNCHRONIZATION
===================================================== */

async function acceptRescueOperation(
    teamId
) {

    try {

        await loadRescueRequests();
        const requests = data.rescueRequests || [];


        const availableRequests =
            requests.filter(
                x =>
                    !x.team_id &&
                    (
                        x.status === 'SOS Pending' ||
                        !x.status
                    )
            );


        if (!availableRequests.length) {

            toast(
                'No pending rescue requests available'
            );

            return;
        }


        let message =
            'Pending Rescue Requests:\n\n';


        availableRequests.forEach(x => {

            message +=

                'Victim ID: ' +
                x.victim_id +

                ' | ' +
                x.name +

                ' | Severity: ' +
                x.severity +

                ' | ' +
                (
                    x.location ||
                    ''
                ) +

                '\n';
        });


        message +=
            '\nEnter Victim ID to accept:';


        const victimId =
            Number(
                prompt(message)
            );


        if (
            !Number.isInteger(victimId) ||
            victimId <= 0
        ) {

            toast(
                'Invalid Victim ID'
            );

            return;
        }


        const request =
            availableRequests.find(
                x =>
                    Number(
                        x.victim_id
                    ) === victimId
            );


        if (!request) {

            toast(
                'Victim ID not found in pending requests'
            );

            return;
        }


        const confirmed =
            confirm(

                'Accept rescue operation?\n\n' +

                'Victim: ' +
                request.name +

                '\nLocation: ' +
                (
                    request.location ||
                    '—'
                ) +

                '\nSeverity: ' +
                request.severity +

                '\nTeam ID: ' +
                teamId

            );


        if (!confirmed) {
            return;
        }


        const response =
            await api(
                '/teams/accept',
                {

                    method: 'PUT',

                    body:
                        JSON.stringify({

                            victimId:
                                victimId,

                            teamId:
                                teamId

                        })

                }
            );


        toast(
            response.message ||
            'Rescue operation accepted'
        );


        await loadAll();


    } catch (error) {

        console.error(
            'Accept rescue error:',
            error
        );

        toast(
            error.message
        );
    }
}


/* =====================================================
   RESCUE FEATURE 3
   UPDATE RESCUE STATUS
   OS: MULTITHREADING
===================================================== */

async function updateRescueStatus(
    teamId
) {

    const statuses = [

        'Assigned',

        'On the Way',

        'Rescue In Progress',

        'Completed',

        'Available'

    ];


    let message =
        'Select Rescue Status:\n\n';


    statuses.forEach(
        (status, index) => {

            message +=
                (index + 1) +
                '. ' +
                status +
                '\n';

        }
    );


    const choice =
        Number(
            prompt(message)
        );


    if (
        !Number.isInteger(choice) ||
        choice < 1 ||
        choice > statuses.length
    ) {

        toast(
            'Invalid status selection'
        );

        return;
    }


    const newStatus =
        statuses[
            choice - 1
        ];


    try {

        const response =
            await api(

                `/teams/${teamId}/status`,

                {

                    method: 'PUT',

                    body:
                        JSON.stringify({

                            status:
                                newStatus

                        })

                }

            );


        toast(
            response.message ||
            'Rescue status updated'
        );


        await loadAll();


    } catch (error) {

        console.error(
            'Status update error:',
            error
        );

        toast(
            error.message
        );
    }
}


/* =====================================================
   RESCUE FEATURE 4
   ACCESS VICTIM LOCATION
   OS: CONTROLLED DATA ACCESS
===================================================== */

async function accessVictimLocation(
    teamId
) {

    try {

        const victim =
            await api(

                `/teams/${teamId}/victim-location`

            );


        alert(

            'VICTIM LOCATION\n\n' +

            'Team ID: ' +
            (
                victim.team_id ||
                '—'
            ) +

            '\nTeam: ' +
            (
                victim.team_name ||
                '—'
            ) +

            '\n\nVictim ID: ' +
            (
                victim.victim_id ||
                '—'
            ) +

            '\nVictim Name: ' +
            (
                victim.victim_name ||
                '—'
            ) +

            '\nPhone: ' +
            (
                victim.phone ||
                '—'
            ) +

            '\n\nLOCATION:\n' +
            (
                victim.location ||
                'Not available'
            ) +

            '\n\nEmergency: ' +
            (
                victim.emergency_type ||
                '—'
            ) +

            '\nSeverity: ' +
            (
                victim.severity ||
                '—'
            ) +

            '\nStatus: ' +
            (
                victim.status ||
                '—'
            )

        );


    } catch (error) {

        toast(
            'Could not access victim location: ' +
            error.message
        );
    }
}


/* =====================================================
   VOLUNTEERS
===================================================== */

function renderVols(a) {

    if (!$('#volRows')) {
        return;
    }


    $('#volRows').innerHTML =

        (a || []).map(x => `

            <tr>

                <td>
                    <b>
                        ${escapeHtml(
                            x.name
                        )}
                    </b>
                </td>

                <td>
                    ${escapeHtml(
                        x.phone || '—'
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        x.skill || '—'
                    )}
                </td>

                <td>
                    ${escapeHtml(
                        x.shelter_name || '—'
                    )}
                </td>

                <td>
                    ${badge(
                        x.availability
                    )}
                </td>

                <td>

                    <button
                        class="ghost small"
                        onclick="toggleVol(
                            ${x.volunteer_id},
                            '${x.availability ===
                            'Available'
                                ? 'Unavailable'
                                : 'Available'}'
                        )">

                        Toggle

                    </button>

                </td>

            </tr>

        `).join('')

        ||

        `

            <tr>

                <td colspan="6">
                    No volunteers yet.
                </td>

            </tr>

        `;
}


/* =====================================================
   SHELTERS
===================================================== */

function renderShelters(a) {

    if (!$('#shelterCards')) {
        return;
    }


    $('#shelterCards').innerHTML =

        (a || []).map(x => `

            <div class="card shelter">

                <div class="shelter-top">

                    <div class="shelter-icon">
                        🏠
                    </div>

                    ${badge(
                        Number(
                            x.available_beds
                        ) > 0
                            ? 'Available'
                            : 'Full'
                    )}

                </div>

                <h3>
                    ${escapeHtml(
                        x.shelter_name
                    )}
                </h3>

                <p>
                    ${escapeHtml(
                        x.address ||
                        x.location ||
                        '—'
                    )}
                </p>

                <div class="bed-number">

                    <b>
                        ${x.available_beds ?? 0}
                    </b>

                    <span>
                        available beds of
                        ${x.capacity ?? 0}
                    </span>

                </div>

                <div class="progress">

                    <span
                        style="width:${
                            x.capacity
                                ? Math.min(
                                    100,
                                    Number(
                                        x.available_beds
                                    ) /
                                    Number(
                                        x.capacity
                                    ) *
                                    100
                                )
                                : 0
                        }%">
                    </span>

                </div>

                <button
                    class="ghost"
                    onclick="assignShelter(
                        ${x.shelter_id}
                    )">

                    Assign to victim

                </button>

            </div>

        `).join('')

        ||

        `

            <div class="card">

                <p>
                    No shelters available.
                </p>

            </div>

        `;
}


/* =====================================================
   RESOURCE REQUESTS
   BACKEND RETURNS ONLY PENDING REQUESTS
===================================================== */

function renderResources(a) {

    if (!$('#resourceRows')) {
        return;
    }


    /*
       IMPORTANT:

       The backend endpoint:

       GET /admin/resource-requests

       already returns ONLY records where:

       resource_request IS NOT NULL
       resource_request is not empty
       resource_status = 'Pending'

       Therefore we do NOT perform another
       frontend status filter here.
    */

    const pendingRequests =
        a || [];


    $('#resourceRows').innerHTML =

        pendingRequests.map(x => {

            let resourceName =
                x.resource_request ||
                x.resource ||
                '—';


            let quantity =
                Number(
                    x.quantity ??
                    x.requested_quantity ??
                    1
                );


            /*
               Database may store:

               Medicine:5
               Food:10
               Water:20
            */

            if (
                typeof resourceName === 'string' &&
                resourceName.includes(':')
            ) {

                const parts =
                    resourceName.split(':');


                resourceName =
                    parts[0].trim();


                const parsedQuantity =
                    Number(
                        parts[1].trim()
                    );


                if (
                    Number.isFinite(
                        parsedQuantity
                    )
                ) {

                    quantity =
                        parsedQuantity;
                }
            }


            return `

                <tr>

                    <td>

                        #${x.victim_id}

                        <b>
                            ${escapeHtml(
                                x.name
                            )}
                        </b>

                    </td>

                    <td>
                        ${escapeHtml(
                            resourceName
                        )}
                    </td>

                    <td>
                        ${quantity}
                    </td>

                    <td>
                        ${badge(
                            x.resource_status ||
                            'Pending'
                        )}
                    </td>

                    <td>

                        <button
                            class="ghost small"
                            onclick="allocate(
                                ${x.victim_id}
                            )">

                            Allocate

                        </button>

                    </td>

                </tr>

            `;

        }).join('')

        ||

        `

            <tr>

                <td colspan="5">

                    No pending resource requests.

                </td>

            </tr>

        `;
}


/* =====================================================
   RESOURCE INVENTORY
===================================================== */

function renderInventory(a) {

    if (!$('#inventoryRows')) {
        return;
    }


    $('#inventoryRows').innerHTML =

        (a || []).map(x => `

            <tr>

                <td>
                    <b>
                        ${escapeHtml(
                            x.resource_name
                        )}
                    </b>
                </td>

                <td>
                    ${x.total_quantity ?? 0}
                </td>

                <td>
                    ${x.allocated_quantity ?? 0}
                </td>

                <td>
                    <b>
                        ${x.remaining_quantity ?? 0}
                    </b>
                </td>

                <td>

                    <button
                        class="ghost small"
                        onclick="addStock(
                            ${x.resource_id},
                            '${String(
                                x.resource_name
                            ).replace(
                                /'/g,
                                "\\'"
                            )}'
                        )">

                        Add Stock

                    </button>

                </td>

            </tr>

        `).join('')

        ||

        `

            <tr>

                <td colspan="5">
                    No resources in inventory.
                </td>

            </tr>

        `;
}


/* =====================================================
   ADD INVENTORY RESOURCE
===================================================== */

async function addInventoryResource() {

    const resourceName =
        prompt(
            'Enter resource name:'
        );


    if (!resourceName) {
        return;
    }


    const quantity =
        Number(
            prompt(
                'Enter total quantity:'
            )
        );


    if (
        !Number.isFinite(quantity) ||
        quantity <= 0
    ) {

        toast(
            'Enter a valid quantity'
        );

        return;
    }


    try {

        await api(
            '/admin/inventory',
            {

                method: 'POST',

                body: JSON.stringify({

                    resourceName:
                        resourceName,

                    quantity:
                        quantity

                })

            }
        );


        toast(
            'Resource added'
        );

        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}
/* =====================================================
   ADD STOCK
===================================================== */

async function addStock(
    id,
    name
) {

    const quantity =
        Number(
            prompt(
                'Add quantity for ' +
                name +
                ':'
            )
        );


    if (
        !Number.isFinite(quantity) ||
        quantity <= 0
    ) {

        toast(
            'Enter a valid quantity'
        );

        return;
    }


    try {

        await api(

            '/admin/inventory/' +
            id +
            '/stock',

            {

                method: 'PUT',

                body: JSON.stringify({

                    quantity:
                        quantity

                })

            }

        );


        toast(
            'Stock added'
        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   FILL SELECTS
===================================================== */

function fillSelects(
    shelters,
    teams
) {

    if ($('#teamSelect')) {

        $('#teamSelect').innerHTML =

            '<option value="">Select available rescue team</option>' +

            (teams || [])
                .filter(
                    x =>
                        x.rescue_status ===
                        'Available'
                )
                .map(x => `

                    <option
                        value="${x.team_id}">

                        #${x.team_id}

                        ${escapeHtml(
                            x.team_name
                        )}

                        —

                        ${escapeHtml(
                            x.leader || ''
                        )}

                        —

                        ${escapeHtml(
                            x.vehicle || ''
                        )}

                    </option>

                `)
                .join('');
    }


    if ($('#volShelter')) {

        $('#volShelter').innerHTML =

            '<option value="0">No shelter</option>' +

            (shelters || [])
                .map(x => `

                    <option
                        value="${x.shelter_id}">

                        ${escapeHtml(
                            x.shelter_name
                        )}

                    </option>

                `)
                .join('');
    }
}


/* =====================================================
   REGISTER VICTIM
===================================================== */

async function registerVictim(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const response =
            await api(
                '/victims',
                {

                    method: 'POST',

                    body:
                        JSON.stringify(
                            Object.fromEntries(
                                form
                            )
                        )

                }
            );


        closeModal(
            'victimModal'
        );


        e.target.reset();


        toast(
            response.message ||
            'Victim registered successfully'
        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   OPEN SOS
===================================================== */

function openSos(id) {

    const input =
        $('#sosModal input[name=id]');


    if (input) {
        input.value = id;
    }


    openModal(
        'sosModal'
    );
}


/* =====================================================
   CREATE SOS
===================================================== */

async function createSos(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const object =
            Object.fromEntries(form);


        const victimId =
            Number(
                object.id
            );


        delete object.id;


        object.victimId =
            victimId;


        await api(

            '/victims/sos',

            {

                method: 'POST',

                body:
                    JSON.stringify(
                        object
                    )

            }

        );


        closeModal(
            'sosModal'
        );


        e.target.reset();


        toast(
            'SOS created successfully'
        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   OPEN TEAM
===================================================== */

function openTeam(id) {

    const input =
        $('#teamModal input[name=victimId]');


    if (input) {
        input.value = id;
    }


    openModal(
        'teamModal'
    );
}


/* =====================================================
   ASSIGN TEAM
===================================================== */

async function assignTeam(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const object =
            Object.fromEntries(form);


        object.victimId =
            Number(
                object.victimId
            );


        object.teamId =
            Number(
                object.teamId
            );


        const response =
            await api(
                '/admin/assign-team',
                {

                    method: 'PUT',

                    body:
                        JSON.stringify(
                            object
                        )

                }
            );


        closeModal(
            'teamModal'
        );


        toast(

            response.message ||
            'Rescue team assigned'

        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   REGISTER RESCUE TEAM
===================================================== */

async function registerRescueTeam(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const object =
            Object.fromEntries(form);


        object.teamId =
            Number(
                object.teamId
            );


        const response =
            await api(
                '/admin/teams',
                {

                    method: 'POST',

                    body:
                        JSON.stringify(
                            object
                        )

                }
            );


        closeModal(
            'rescueTeamModal'
        );


        e.target.reset();


        toast(

            response.message ||
            'Rescue team registered'

        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   REQUEST RESOURCE
===================================================== */

async function requestResource(id) {

    try {

        const inventory =
            await api(
                '/admin/inventory'
            );


        const available =
            (inventory || []).filter(
                x =>
                    Number(
                        x.remaining_quantity
                    ) > 0
            );


        if (!available.length) {

            toast(
                'No resources are currently available'
            );

            return;
        }


        const select =
            $('#adminResourceSelect');


        if (!select) {

            toast(
                'Resource request form not found'
            );

            return;
        }


        select.innerHTML =

            '<option value="">Select resource</option>' +

            available
                .map(x => `

                    <option
                        value="${escapeHtml(
                            x.resource_name
                        )}"
                        data-available="${
                            x.remaining_quantity
                        }">

                        ${escapeHtml(
                            x.resource_name
                        )}

                        —

                        ${x.remaining_quantity}
                        available

                    </option>

                `)
                .join('');


        const victimIdInput =
            $('#resourceRequestModal input[name=victimId]');


        if (victimIdInput) {

            victimIdInput.value =
                id;
        }


        openModal(
            'resourceRequestModal'
        );


    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ADMIN RESOURCE REQUEST
===================================================== */

async function submitAdminResourceRequest(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const victimId =
            Number(
                form.get('victimId')
            );


        const resource =
            form.get('resource');


        const quantity =
            Number(
                form.get('quantity')
            );


        if (!victimId) {

            toast(
                'Invalid victim'
            );

            return;
        }


        if (!resource) {

            toast(
                'Please select a resource'
            );

            return;
        }


        if (
            !Number.isInteger(quantity) ||
            quantity <= 0
        ) {

            toast(
                'Please enter a valid quantity'
            );

            return;
        }


        await api(

            `/victims/${victimId}/resource`,

            {

                method: 'PUT',

                body:
                    JSON.stringify({

                        resource:
                            resource,

                        quantity:
                            quantity

                    })

            }

        );


        closeModal(
            'resourceRequestModal'
        );


        e.target.reset();


        toast(
            'Resource request sent successfully'
        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ALLOCATE RESOURCE
===================================================== */

async function allocate(id) {

    try {

        const confirmed =
            confirm(
                'Allocate resources to Victim #' +
                id +
                '?\n\nAfter allocation, this request will be removed from the pending request list.'
            );


        if (!confirmed) {
            return;
        }


        const response =
            await api(

                '/admin/resources/' +
                id,

                {

                    method: 'PUT'

                }

            );


        toast(
            response.message ||
            'Resources allocated successfully'
        );


        /*
           IMPORTANT:

           Reloading data calls:

           GET /admin/resource-requests

           The backend returns ONLY:

           resource_status = 'Pending'

           Therefore the allocated request
           automatically disappears.
        */

        await loadAll();


    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   VOLUNTEER
===================================================== */

async function addVolunteer(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const object =
            Object.fromEntries(form);


        if (
            object.shelterId === '' ||
            object.shelterId === '0'
        ) {

            delete object.shelterId;

        } else {

            object.shelterId =
                Number(
                    object.shelterId
                );
        }


        await api(
            '/volunteers',
            {

                method: 'POST',

                body:
                    JSON.stringify(
                        object
                    )

            }
        );


        closeModal(
            'volunteerModal'
        );


        e.target.reset();


        toast(
            'Volunteer added'
        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   TOGGLE VOLUNTEER
===================================================== */

async function toggleVol(
    id,
    availability
) {

    try {

        await api(

            `/volunteers/${id}/availability`,

            {

                method: 'PUT',

                body:
                    JSON.stringify({

                        availability:
                            availability

                    })

            }

        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ASSIGN SHELTER
===================================================== */

async function assignShelter(id) {

    const victim =
        prompt(
            'Enter Victim ID to assign this shelter:'
        );


    if (!victim) {
        return;
    }


    const victimId =
        Number(victim);


    if (
        !Number.isInteger(victimId) ||
        victimId <= 0
    ) {

        toast(
            'Invalid Victim ID'
        );

        return;
    }


    try {

        await api(
            '/admin/shelter',
            {

                method: 'PUT',

                body:
                    JSON.stringify({

                        victimId:
                            victimId,

                        shelterId:
                            id

                    })

            }
        );


        toast(
            'Shelter assigned'
        );


        await loadAll();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   REPORT
===================================================== */

async function loadReport() {

    try {

        const report =
            await api(
                '/report'
            );


        if (!$('#reportRows')) {
            return;
        }


        $('#reportRows').innerHTML =

            (report || []).map(x => `

                <tr>

                    <td>

                        #${x.victim_id}

                        ${escapeHtml(
                            x.victim_name ||
                            x.name ||
                            ''
                        )}

                    </td>

                    <td>
                        ${escapeHtml(
                            x.location ||
                            '—'
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.emergency_type ||
                            '—'
                        )}
                    </td>

                    <td>
                        ${x.severity || '—'}
                    </td>

                    <td>
                        ${badge(
                            x.status
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.team_name ||
                            '—'
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.shelter_name ||
                            '—'
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            x.resource_status ||
                            '—'
                        )}
                    </td>

                </tr>

            `).join('')

        ||

            `

                <tr>

                    <td colspan="8">
                        No report data available.
                    </td>

                </tr>

            `;


    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   LOAD LIVE SOS INTO PRIORITY SCHEDULER
===================================================== */

async function loadSosForScheduling() {
    try {
        const requests = await api('/admin/sos');

        if (!requests.length) {
            toast('There are no pending SOS requests to schedule.');
            return;
        }

        const input = $('#priorityInput');
        if (!input) {
            toast('Priority scheduling input not found.');
            return;
        }

        // Format: process ID, burst time, priority, arrival time.
        // Use severity as priority and victim ID order as the FCFS tie-break.
        input.value = requests
            .map((request, index) =>
                `P${request.victim_id},1,${Number(request.severity) || 1},${index}`
            )
            .join('\\n');

        go('scheduling');
        toast(`Loaded ${requests.length} pending SOS request(s). Run Priority to calculate the schedule.`);
    } catch (error) {
        toast('Could not load pending SOS requests: ' + error.message);
    }
}


/* =====================================================
   OS SCHEDULING
===================================================== */

async function runSchedule(type) {

    try {

        const inputId =
            type === 'fcfs'
                ? 'fcfsInput'
                : 'priorityInput';


        const input =
            $('#' + inputId);


        if (!input) {

            toast(
                'Scheduling input not found'
            );

            return;
        }


        const raw =
            input.value
                .trim()
                .split('\n')
                .filter(Boolean);


        const jobs =
            raw.map((line, index) => {

                const parts =
                    line
                        .split(',')
                        .map(
                            x => x.trim()
                        );


                // Accept both numeric IDs (1) and labels (P1).
                const pidMatch = String(
                    parts[0] || (index + 1)
                ).match(/-?\\d+/);

                const pid = pidMatch
                    ? Number(pidMatch[0])
                    : index + 1;

                if (type === 'fcfs') {
                    return {
                        pid,
                        arrival: Number(parts[1]),
                        burst: Number(parts[2])
                    };
                }

                return {
                    pid,
                    arrival: parts.length > 3
                        ? Number(parts[3])
                        : 0,
                    burst: Number(parts[1]),
                    priority: Number(parts[2])
                };

            });


        const invalidJob = jobs.some(job =>
            !Number.isInteger(job.pid) ||
            !Number.isFinite(job.burst) ||
            job.burst <= 0 ||
            (type === 'fcfs' &&
                (!Number.isFinite(job.arrival) || job.arrival < 0)) ||
            (type === 'priority' &&
                (!Number.isFinite(job.priority) ||
                 !Number.isFinite(job.arrival) ||
                 job.arrival < 0))
        );

        if (invalidJob) {
            toast(
                type === 'fcfs'
                    ? 'Use ID, arrival time, burst time; burst must be positive.'
                    : 'Use ID, burst time, priority[, arrival time]; burst must be positive.'
            );
            return;
        }

        const response =
            await api(

                '/scheduling/' +
                type,

                {

                    method: 'POST',

                    body:
                        JSON.stringify(
                            jobs
                        )

                }

            );


        const avgWaiting =
            response.averageWaitingTime ??
            response.avgWaiting ??
            0;


        const avgTurnaround =
            response.averageTurnaroundTime ??
            response.avgTurnaround ??
            0;


        if ($('#scheduleResult')) {

            const resultJobs =
                response.jobs ||
                response.processes ||
                [];


            $('#scheduleResult').innerHTML = `

                <h3>
                    Scheduling result
                </h3>


                <div class="schedule-metrics">

                    <span>

                        Average waiting

                        <b>
                            ${Number(
                                avgWaiting
                            ).toFixed(2)}
                        </b>

                    </span>


                    <span>

                        Average turnaround

                        <b>
                            ${Number(
                                avgTurnaround
                            ).toFixed(2)}
                        </b>

                    </span>

                </div>


                <div class="tablewrap">

                    <table>

                        <thead>

                            <tr>

                                ${
                                    Object.keys(
                                        resultJobs[0] || {}
                                    )
                                    .map(key => `

                                        <th>
                                            ${escapeHtml(
                                                key
                                            )}
                                        </th>

                                    `)
                                    .join('')
                                }

                            </tr>

                        </thead>


                        <tbody>

                            ${
                                resultJobs
                                    .map(job => `

                                        <tr>

                                            ${
                                                Object.values(
                                                    job
                                                )
                                                .map(value => `

                                                    <td>
                                                        ${escapeHtml(
                                                            value
                                                        )}
                                                    </td>

                                                `)
                                                .join('')
                                            }

                                        </tr>

                                    `)
                                    .join('')
                            }

                        </tbody>

                    </table>

                </div>

            `;
        }


    } catch (error) {

        toast(
            error.message
        );
    }
}

/* =====================================================
   VICTIM LIVE SOS AND RESCUE TRACKING
===================================================== */

function renderVictimTracking(track) {
    const container = $('#victimTracking');

    if (!container) return;

    if (!track) {
        container.innerHTML = `
            <p class="muted">
                Tracking information is not available yet.
            </p>
        `;
        return;
    }

    const sosStatus =
        track.status ||
        track.sos_status ||
        'Registered';

    const teamId =
        track.team_id ??
        track.teamId ??
        null;

    const teamName =
        track.team_name ||
        track.teamName ||
        (teamId ? `Team #${teamId}` : 'Not assigned');

    const rescueStatus =
        track.rescue_status ||
        track.rescueStatus ||
        (teamId ? 'Team assigned' : 'Waiting for assignment');

    const resourceStatus =
        track.resource_status ||
        track.resourceStatus ||
        'No resource request';

    const stages = [
        {
            title: 'SOS submitted',
            description: 'Your emergency request has been recorded.',
            complete: true
        },
        {
            title: 'Command center review',
            description: sosStatus,
            complete: ![
                'Registered',
                'SOS Pending',
                'Pending'
            ].includes(sosStatus)
        },
        {
            title: 'Rescue team',
            description: `${teamName} · ${rescueStatus}`,
            complete: Boolean(teamId)
        },
        {
            title: 'Emergency resolution',
            description: sosStatus,
            complete: /resolved|completed/i.test(sosStatus)
        }
    ];

    container.innerHTML = `
        <div class="status-list">
            <div>
                <span>SOS status</span>
                <b>${escapeHtml(sosStatus)}</b>
            </div>

            <div>
                <span>Rescue team</span>
                <b>${escapeHtml(teamName)}</b>
            </div>

            <div>
                <span>Rescue progress</span>
                <b>${escapeHtml(rescueStatus)}</b>
            </div>

            <div>
                <span>Resource request</span>
                <b>${escapeHtml(resourceStatus)}</b>
            </div>
        </div>

        <div class="victim-tracking-stages" style="margin-top:16px;">
            ${stages.map((stage, index) => `
                <div style="
                    display:flex;
                    gap:12px;
                    align-items:flex-start;
                    margin:12px 0;
                ">
                    <span style="
                        display:flex;
                        align-items:center;
                        justify-content:center;
                        min-width:28px;
                        height:28px;
                        border-radius:50%;
                        background:${stage.complete
                            ? '#d1fae5'
                            : '#e5e7eb'};
                        color:${stage.complete
                            ? '#065f46'
                            : '#374151'};
                        font-weight:700;
                    ">
                        ${stage.complete ? '✓' : index + 1}
                    </span>

                    <div>
                        <b>${escapeHtml(stage.title)}</b>
                        <p class="muted" style="margin:4px 0;">
                            ${escapeHtml(stage.description)}
                        </p>
                    </div>
                </div>
            `).join('')}
        </div>

        <p class="muted" style="margin-top:12px;">
            Tracking refreshes automatically while you are signed in.
        </p>
    `;
}


/* =====================================================
   VICTIM RESOURCE REQUEST HISTORY
===================================================== */

function renderVictimResourceHistory(history) {
    const container = $('#victimResourceHistory');

    if (!container) return;

    const requests = Array.isArray(history)
        ? history
        : Array.isArray(history?.history)
            ? history.history
            : Array.isArray(history?.requests)
                ? history.requests
                : [];

    if (!requests.length) {
        container.innerHTML = `
            <p class="muted">
                You have not submitted any resource requests yet.
            </p>
        `;
        return;
    }

    container.innerHTML = `
        <div class="tablewrap">
            <table>
                <thead>
                    <tr>
                        <th>Resource</th>
                        <th>Quantity</th>
                        <th>Status</th>
                        <th>Requested on</th>
                    </tr>
                </thead>
                <tbody>
                    ${requests.map(item => `
                        <tr>
                            <td>
                                ${escapeHtml(
                                    item.resource_name ||
                                    item.resource ||
                                    item.resource_request ||
                                    '—'
                                )}
                            </td>

                            <td>
                                ${escapeHtml(
                                    String(
                                        item.quantity ??
                                        item.requested_quantity ??
                                        '—'
                                    )
                                )}
                            </td>

                            <td>
                                ${badge(
                                    item.status ||
                                    item.resource_status ||
                                    'Pending'
                                )}
                            </td>

                            <td>
                                ${escapeHtml(
                                    formatRequestDate(
                                        item.created_at ||
                                        item.request_date ||
                                        item.requested_at
                                    )
                                )}
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}


function formatRequestDate(value) {
    if (!value) return '—';

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return String(value);
    }

    return date.toLocaleString();
}

/* =====================================================
   VICTIM PORTAL
===================================================== */

async function loadVictimPortal() {
    const id =
        session?.user?.victim_id ??
        session?.user?.victimId;

    if (!id) {
        toast('Victim session is invalid');
        return;
    }

    try {
        const [victimResult, shelterResult] =
            await Promise.allSettled([
                api(`/victim/${id}/full`),
                api('/shelters')
            ]);

        if (victimResult.status === 'rejected') {
            throw victimResult.reason;
        }

        const victim = victimResult.value;

        const shelters = shelterResult.status === 'fulfilled'
            ? shelterResult.value
            : [];

        session.user = {
            ...session.user,
            ...victim
        };

        localStorage.setItem(
            'resqsimSession',
            JSON.stringify(session)
        );

        if ($('#vName')) {
            $('#vName').textContent = `Hello, ${victim.name || 'Victim'}`;
        }

        if ($('#vStatus')) {
            $('#vStatus').textContent =
                victim.status || 'Registered';
        }

        if ($('#victimStatusCard')) {
            $('#victimStatusCard').innerHTML = `
                <div class="status-list">
                    <div>
                        <span>Emergency</span>
                        <b>${escapeHtml(
                            victim.emergency_type || 'No SOS yet'
                        )}</b>
                    </div>

                    <div>
                        <span>Severity</span>
                        <b>${escapeHtml(
                            String(victim.severity ?? '—')
                        )}</b>
                    </div>

                    <div>
                        <span>Rescue team</span>
                        <b>${escapeHtml(
                            victim.team_name ||
                            (victim.team_id
                                ? `Team #${victim.team_id}`
                                : 'Not assigned')
                        )}</b>
                    </div>

                    <div>
                        <span>Shelter</span>
                        <b>${escapeHtml(
                            victim.shelter_name || 'Not assigned'
                        )}</b>
                    </div>

                    <div>
                        <span>Resources</span>
                        <b>${escapeHtml(
                            victim.resource_status || 'No request'
                        )}</b>
                    </div>
                </div>
            `;
        }

        if ($('#victimShelters')) {
            $('#victimShelters').innerHTML =
                (shelters || []).slice(0, 4).map(shelter => `
                    <div class="shelter-mini">
                        <b>${escapeHtml(
                            shelter.shelter_name || 'Shelter'
                        )}</b>

                        <span>
                            ${escapeHtml(
                                String(shelter.available_beds ?? 0)
                            )}
                            beds available
                        </span>
                    </div>
                `).join('') || `
                    <p class="muted">
                        No shelter currently has capacity.
                    </p>
                `;
        }

        // Tracking and history are loaded separately so that
        // a failure in either does not prevent the main portal loading.
        await Promise.allSettled([
            loadVictimTracking(id),
            loadVictimResourceHistory(id),
            loadVictimResourceOptions()
        ]);

    } catch (error) {
        console.error('Victim portal error:', error);
        toast(error.message || 'Could not load victim portal');
    }
}


/* =====================================================
   LOAD LIVE TRACKING FROM BACKEND
===================================================== */

async function loadVictimTracking(victimId) {
    const container = $('#victimTracking');

    if (!container) return;

    try {
        const tracking = await api(
            `/victims/${victimId}/track`
        );

        renderVictimTracking(tracking);
    } catch (error) {
        console.error('Tracking error:', error);

        container.innerHTML = `
            <p class="muted">
                Live tracking could not be loaded.
                Please refresh or try again later.
            </p>
        `;
    }
}


/* =====================================================
   LOAD RESOURCE REQUEST HISTORY FROM BACKEND
===================================================== */

async function loadVictimResourceHistory(victimId) {
    const container = $('#victimResourceHistory');

    if (!container) return;

    try {
        const history = await api(
            `/victims/${victimId}/resource-history`
        );

        renderVictimResourceHistory(history);
    } catch (error) {
        console.error('Resource history error:', error);

        container.innerHTML = `
            <p class="muted">
                Resource history could not be loaded.
                Check that the history endpoint is available in the backend.
            </p>
        `;
    }
}

/* =====================================================
   LOAD VICTIM RESOURCE OPTIONS
===================================================== */

async function loadVictimResourceOptions() {

    const resourceSelect =
        $('#resourceForm select[name="resource"]');


    if (!resourceSelect) {
        return;
    }


    try {

        const inventory =
            await api(
                '/admin/inventory'
            );


        const available =
            (inventory || []).filter(
                x =>
                    Number(
                        x.remaining_quantity
                    ) > 0
            );


        resourceSelect.innerHTML =

            '<option value="">Select resource</option>' +

            available
                .map(x => `

                    <option
                        value="${escapeHtml(
                            x.resource_name
                        )}"
                        data-available="${
                            x.remaining_quantity
                        }">

                        ${escapeHtml(
                            x.resource_name
                        )}

                        —

                        ${x.remaining_quantity}
                        available

                    </option>

                `)
                .join('');


        if (!available.length) {

            resourceSelect.innerHTML =
                '<option value="">No resources available</option>';
        }


    } catch (error) {

        console.error(
            'Could not load resources:',
            error
        );


        resourceSelect.innerHTML =
            '<option value="">Unable to load resources</option>';
    }
}


/* =====================================================
   VICTIM RESOURCE FORM
===================================================== */

const resourceForm =
    $('#resourceForm');


if (resourceForm) {

    resourceForm.onsubmit =
        async e => {

            e.preventDefault();

            try {

                const form =
                    new FormData(
                        e.target
                    );


                const resource =
                    form.get('resource');


                const quantity =
                    Number(
                        form.get('quantity')
                    );


                if (!resource) {

                    toast(
                        'Please select a resource'
                    );

                    return;
                }


                if (
                    !Number.isInteger(quantity) ||
                    quantity <= 0
                ) {

                    toast(
                        'Please enter a valid quantity'
                    );

                    return;
                }


                const select =
                    e.target.querySelector(
                        'select[name="resource"]'
                    );


                const selectedOption =
                    select?.options[
                        select.selectedIndex
                    ];


                const available =
                    Number(
                        selectedOption
                            ?.dataset
                            ?.available
                    );


                if (
                    Number.isFinite(
                        available
                    ) &&
                    quantity > available
                ) {

                    toast(
                        `Only ${available} units are available`
                    );

                    return;
                }


                await api(

                    `/victims/${
                        session.user.victim_id
                    }/resource`,

                    {

                        method: 'PUT',

                        body:
                            JSON.stringify({

                                resource:
                                    resource,

                                quantity:
                                    quantity

                            })

                    }

                );


                e.target.reset();


                toast(
                    'Resource request submitted'
                );


                await loadVictimPortal();

            } catch (error) {

                toast(
                    error.message
                );
            }
        };
}


/* =====================================================
   VICTIM SOS
===================================================== */

async function victimSendSos(e) {

    e.preventDefault();

    try {

        const form =
            new FormData(e.target);


        const object =
            Object.fromEntries(
                form
            );


        object.victimId =
            session.user.victim_id;


        await api(

            '/victims/sos',

            {

                method: 'POST',

                body:
                    JSON.stringify(
                        object
                    )

            }

        );


        closeModal(
            'victimSosModal'
        );


        e.target.reset();


        toast(
            'Emergency SOS sent to command center'
        );


        await loadVictimPortal();

    } catch (error) {

        toast(
            error.message
        );
    }
}


/* =====================================================
   ESCAPE HTML
===================================================== */

function escapeHtml(value) {

    return String(
        value ?? ''
    )

        .replace(
            /&/g,
            '&amp;'
        )

        .replace(
            /</g,
            '&lt;'
        )

        .replace(
            />/g,
            '&gt;'
        )

        .replace(
            /"/g,
            '&quot;'
        )

        .replace(
            /'/g,
            '&#039;'
        );
}

/* =====================================================
   VICTIM PORTAL AUTO REFRESH
===================================================== */

let victimRefreshTimer = null;

function startVictimAutoRefresh() {
    stopVictimAutoRefresh();

    if (session?.role !== 'victim') return;

    victimRefreshTimer = setInterval(() => {
        if (
            session?.role === 'victim' &&
            !document.hidden
        ) {
            loadVictimPortal();
        }
    }, 15000);
}

function stopVictimAutoRefresh() {
    if (victimRefreshTimer !== null) {
        clearInterval(victimRefreshTimer);
        victimRefreshTimer = null;
    }
}

document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        stopVictimAutoRefresh();
    } else if (session?.role === 'victim') {
        loadVictimPortal();
        startVictimAutoRefresh();
    }
});


/* =====================================================
   START APPLICATION
===================================================== */

if (session) {
    showApp();
}

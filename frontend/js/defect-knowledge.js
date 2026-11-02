/**
 * TRACKVISION GUARD – Railway Engineering Defect Knowledge Base
 * Phase 4: Structured Engineering Explanations, Causes, Risks, Precautions, Recommendations & Prevention.
 */

const RAILWAY_DEFECT_KNOWLEDGE = {
    "broken_rail": {
        categoryTitle: "Broken Rail / Transverse Fracture",
        family: "Structural Fracture",
        characteristics: "Complete or partial structural discontinuity extending across the rail head, web, or foot. Characterized by visible separation or jagged fracture surfaces where rail continuity is disrupted.",
        limitations: "Visual surface analysis detects macroscopic surface separation only; it cannot determine internal fatigue origin depth or residual metallurgical stress without ultrasonic testing (UT).",
        causes: [
            "Severe rolling-contact fatigue (RCF) propagating from internal manufacturing inclusions or micro-fissures.",
            "Heavy axle-load impacts combined with thermal tensile stresses during cold weather contraction.",
            "Unsupported or pumping sleepers causing high cyclic bending moments.",
            "Hydrogen embrittlement or localized metallurgical defects from improper field thermite welds."
        ],
        risks: [
            "Immediate threat of catastrophic wheel derailment upon dynamic train traversal.",
            "Disruption of electrical track circuit signaling, leading to false-clear signal indications.",
            "Rapid gauge widening and lateral rail turnover under dynamic bogie hunting loads."
        ],
        precautions: [
            "IMMEDIATE ACTION: Notify railway traffic control and halt train movements on this track section.",
            "Never allow unauthorized personnel to enter the active permanent way.",
            "Place emergency detonators and stop-boards according to Railway Permanent Way (P-Way) Safety Rules.",
            "Do not attempt temporary clamping without certified railway engineering supervision."
        ],
        recommendations: [
            "Emergency installation of certified emergency rail clamps (fishplates with joggled clamps) under speed restriction if permitted by local rules.",
            "Cut out the defective rail segment (minimum 6-meter rail replacement) and execute certified thermite or flash-butt welding.",
            "Perform post-weld ultrasonic non-destructive testing (NDT) across the replaced rail and adjacent parent rail."
        ],
        prevention: [
            "Enforce periodic ultrasonic rail flaw detection (USFD) vehicle runs at specified GMT (Gross Million Tonnes) intervals.",
            "Maintain correct rail neutral temperature during de-stressing operations to prevent excessive winter tensile stress.",
            "Ensure regular ballast tamping to eliminate sleeper voids and unsupported rail joints."
        ]
    },
    "railway_join_bar_defects": {
        categoryTitle: "Railway Joint Bar (Fishplate) Defect",
        family: "Joint Assembly",
        characteristics: "Visible cracking across the fishplate middle, bolt-hole deformation, loosened fasteners, or vertical step alignment at the rail rail interface.",
        limitations: "Visual inspection assesses external joint assembly and visible bolt condition; internal rail-end bolt-hole star cracks require ultrasonic inspection.",
        causes: [
            "High impact forces caused by wheel passage over the expansion rail gap.",
            "Loose, rusted, or sheared fishbolt nuts and lock washers due to track vibration.",
            "Thermal expansion constraints causing excessive rail-end binding or joint opening.",
            "Sub-standard fishplate metallography or fatigue under repetitive dynamic axle impacts."
        ],
        risks: [
            "Development of progressive fishplate fractures resulting in unguided rail ends.",
            "Vertical and lateral track misalignments leading to flange climb derailments.",
            "Accelerated battering of rail ends requiring premature rail-end cutting."
        ],
        precautions: [
            "Impose temporary operational speed restrictions until the joint is examined by a permanent way supervisor.",
            "Check both inner and outer fishplates and all 4 or 6 through-bolts.",
            "Record joint gap measurement with a calibrated feeler gauge and note ambient rail temperature.",
            "Ensure personnel work with approved safety lookouts."
        ],
        recommendations: [
            "Torque all joint bolts to specified engineering limits using a calibrated torque wrench.",
            "Replace worn, bent, or cracked fishplates with heavy-duty certified joint bars.",
            "Weld and profile rail-end batter using approved hard-facing electrode techniques."
        ],
        prevention: [
            "Scheduled seasonal joint lubrication and bolt tightening programs.",
            "Gradual replacement of traditional bolted joints with Continuously Welded Rail (CWR) where applicable.",
            "Maintain joint sleepers and pack ballast firmly to minimize dynamic joint deflection."
        ]
    },
    "corrosion": {
        categoryTitle: "Rail Corrosion & Oxidation Deterioration",
        family: "Material Degradation",
        characteristics: "Oxidative rust scaling, surface pitting, and dimensional reduction of the rail cross-section, especially prevalent on the rail foot, web, and underneath fastening clips.",
        limitations: "Surface photographic inspection identifies rust scale and area coverage; remaining web/foot thickness must be measured using ultrasonic thickness gauges.",
        causes: [
            "Prolonged exposure to trapped moisture, wet ballast, and standing water in poorly drained sections.",
            "Chemical exposure in coastal marine environments, industrial corridors, or toilet discharge areas.",
            "Stray DC traction currents causing electrolytic galvanic corrosion.",
            "Accumulation of moist dirt and decaying vegetation against the rail web."
        ],
        risks: [
            "Reduction in cross-sectional moment of inertia, significantly decreasing the rail's load-carrying capacity.",
            "Pitting corrosion serving as severe stress-concentration points for transverse fatigue fractures.",
            "Corrosion fatigue causing premature rail foot breakages under dynamic bending tension."
        ],
        precautions: [
            "Schedule thorough thickness measurement by permanent way maintenance personnel.",
            "Clean surrounding debris and ensure free drainage around the rail foot.",
            "Avoid walking on slippery, loose ballast beds."
        ],
        recommendations: [
            "Clean corroded sections by wire brushing or mechanical descaling to determine actual metal loss.",
            "Measure remaining rail foot and web thickness; condemn rail if loss exceeds safety limits.",
            "Apply anti-corrosive epoxy mastic coatings or bitumen paint per railway standard specifications."
        ],
        prevention: [
            "Provide and maintain clean ballast shoulders and clear track drainage channels.",
            "Use corrosion-resistant zinc-coated rail clips and composite anti-corrosive pads.",
            "Establish cyclic anti-corrosive painting programs in corrosive marine and industrial zones."
        ]
    },
    "head_checks": {
        categoryTitle: "Head Checks / Gauge-Corner RCF Cracking",
        family: "Rolling Contact Fatigue (RCF)",
        characteristics: "Series of closely-spaced, fine, parallel hairline cracks angled at 30°–60° along the gauge corner of the high rail on curved track.",
        limitations: "Visual cameras observe the surface crack density; ultrasonic angle-beam or eddy-current testing is required to determine whether cracks have branched downward.",
        causes: [
            "Repetitive, severe contact shear stresses exceeding the plastic yield limit of the rail steel during wheel-flange negotiation.",
            "Insufficient rail-wheel conformal contact geometry or tight curve radii.",
            "Inadequate or delayed cyclic rail grinding intervals allowing micro-cracks to deepen.",
            "High friction conditions at the wheel-rail interface."
        ],
        risks: [
            "Branching and downward propagation into dangerous transverse defect fractures.",
            "Spalling and deep flaking of the gauge corner, impairing wheel steering.",
            "Sudden rail head breakage under high-speed heavy haul traffic."
        ],
        precautions: [
            "Schedule detailed eddy current or angle-beam ultrasonic testing to determine crack depth.",
            "Record exact curve radius, track elevation, and rail mileage.",
            "Follow standard speed monitoring rules if cracks exceed threshold depth."
        ],
        recommendations: [
            "Deploy specialized rail grinding train to remove the cracked work-hardened surface layer before cracks reach critical depth.",
            "Optimize top-of-rail friction modifiers and gauge-face curve lubricators.",
            "If crack depth exceeds the permissible grinding envelope, program the rail for replacement."
        ],
        prevention: [
            "Implement proactive cyclic profile grinding per GMT throughput.",
            "Install automated track-side curve lubricators to reduce friction and contact shear stress.",
            "Utilize head-hardened (HH) or heat-treated rail grades on tight curves."
        ]
    },
    "squats": {
        categoryTitle: "Squat Defect / Localized Rolling Contact Fatigue",
        family: "Rolling Contact Fatigue (RCF)",
        characteristics: "Localized widening and depression of the running contact band, accompanied by a kidney-shaped dark imprint and crescent-shaped cracks radiating outwards.",
        limitations: "Surface visual inspection detects top indentation; internal horizontal and downward oblique crack delamination requires ultrasonic B-scan validation.",
        causes: [
            "Wheel slip/slide events causing white etching layers (WEL) and martensitic transformation on the rail crown.",
            "Micro-structural subsurface shear fatigue under repeated high wheel impact loads.",
            "Localized metallurgical defects combined with cyclic dynamic wheel impacts."
        ],
        risks: [
            "Downward crack turning, developing into a transverse defect that can cause catastrophic rail break.",
            "Intense dynamic impact generation, causing damage to train wheels and track fastenings.",
            "Progressive material loss causing deep rail head dips and high track geometry roughness."
        ],
        precautions: [
            "Mark defect location with railway safety paint and record chainage.",
            "Initiate ultrasonic testing within the prescribed railway maintenance timeframe.",
            "Monitor defect progression weekly if left in track under temporary speed caution."
        ],
        recommendations: [
            "Evaluate defect with ultrasonic B-scan testing to verify if crack has turned downward.",
            "Execute rail surface milling or corrective rail grinding if the defect is shallow.",
            "If ultrasonic test shows downward crack growth, replace the rail segment without delay."
        ],
        prevention: [
            "Prevent wheel burn events through traction control optimization on locomotives.",
            "Periodic rail reprofiling before white etching layers develop into rolling contact fatigue cracks.",
            "Regular track geometry monitoring to avoid localized dynamic overloading."
        ]
    },
    "cracks": {
        categoryTitle: "Rail Surface Cracks & Fissures",
        family: "Surface Damage",
        characteristics: "Visible longitudinal, transverse, or oblique fissures on the rail crown or gauge face.",
        limitations: "Visual imaging cannot determine subsurface propagation angle or internal crack depth.",
        causes: ["Rolling contact fatigue, manufacturing micro-inclusions, or heavy thermal-mechanical stress."],
        risks: ["Crack propagation leading to rail break under repetitive dynamic loading."],
        precautions: ["Arrange urgent non-destructive testing and maintain strict track monitoring."],
        recommendations: ["Perform ultrasonic testing, rail grinding for shallow fissures, or rail replacement."],
        prevention: ["Scheduled ultrasonic inspections, profile grinding, and track geometry stabilization."]
    },
    "surface_damage": {
        categoryTitle: "Surface Damage, Spalling & Flaking",
        family: "Surface Irregularity",
        characteristics: "Metallic surface spalling, flaking, or gouging on the rail running band.",
        limitations: "Depth of work-hardening and subsurface micro-cracks cannot be determined visually.",
        causes: ["Wheel burn, severe slip events, foreign object impact, or heavy rolling fatigue."],
        risks: ["Accelerated dynamic impact, damage to adjacent fastenings, and localized dips."],
        precautions: ["Inspect adjacent fastenings and verify track vertical profile."],
        recommendations: ["Mechanical grinding or weld restoration if permitted by engineering standards."],
        prevention: ["Locomotive wheel-slip control, friction management, and regular track cleaning."]
    },
    "fasteners": {
        categoryTitle: "Fastening System Defect / Missing Fasteners",
        family: "Track Fastening",
        characteristics: "Displaced, broken, or missing elastic rail clips, rubber pads, or rail anchors.",
        limitations: "Visual verification does not measure clip toe load tension directly.",
        causes: ["Severe vibration, track dynamic pumping, clip fatigue, or vandalism."],
        risks: ["Loss of rail gauge restraint, increased risk of track spreading and derailment."],
        precautions: ["Check gauge measurement immediately; do not allow consecutive missing clips."],
        recommendations: ["Replace missing or fatigued clips and check insulating liners and pads."],
        prevention: ["Cyclic tightening and toe-load audits using certified torque measurement."]
    },
    "sleepers": {
        categoryTitle: "Sleeper (Tie) Degradation",
        family: "Substructure Support",
        characteristics: "Cracking, spalling, or pulverized concrete beneath the rail seat on concrete ties, or rot on timber.",
        limitations: "Sub-ballast ballast-voiding conditions require dynamic track recording car data.",
        causes: ["Unsupported voiding ballast, excessive dynamic wheel impact, or water logging."],
        risks: ["Loss of vertical support, dynamic track settlement, and rail fatigue."],
        precautions: ["Inspect surrounding sleepers; verify if voiding ballast exists."],
        recommendations: ["Pack ballast or perform mechanized tamping; replace damaged sleepers."],
        prevention: ["Effective shoulder ballast cleaning and cyclic mechanized track tamping."]
    }
};

/**
 * Resolves comprehensive engineering guidance for a given raw class name or defect category
 */
function resolveDefectKnowledge(rawClassName) {
    if (!rawClassName) return RAILWAY_DEFECT_KNOWLEDGE["surface_damage"];
    const c = rawClassName.toLowerCase();

    if (c.includes("broken") || c.includes("fracture")) return RAILWAY_DEFECT_KNOWLEDGE["broken_rail"];
    if (c.includes("join") || c.includes("joint") || c.includes("fishplate")) return RAILWAY_DEFECT_KNOWLEDGE["railway_join_bar_defects"];
    if (c.includes("corrosion") || c.includes("rust")) return RAILWAY_DEFECT_KNOWLEDGE["corrosion"];
    if (c.includes("head_check") || c.includes("head check")) return RAILWAY_DEFECT_KNOWLEDGE["head_checks"];
    if (c.includes("squat")) return RAILWAY_DEFECT_KNOWLEDGE["squats"];
    if (c.includes("crack")) return RAILWAY_DEFECT_KNOWLEDGE["cracks"];
    if (c.includes("fastener") || c.includes("clip")) return RAILWAY_DEFECT_KNOWLEDGE["fasteners"];
    if (c.includes("sleeper") || c.includes("tie")) return RAILWAY_DEFECT_KNOWLEDGE["sleepers"];
    if (c.includes("spall") || c.includes("flake") || c.includes("surface")) return RAILWAY_DEFECT_KNOWLEDGE["surface_damage"];

    return RAILWAY_DEFECT_KNOWLEDGE["surface_damage"];
}

// Expose globally for browser scripts
if (typeof window !== 'undefined') {
    window.RAILWAY_DEFECT_KNOWLEDGE = RAILWAY_DEFECT_KNOWLEDGE;
    window.resolveDefectKnowledge = resolveDefectKnowledge;
}


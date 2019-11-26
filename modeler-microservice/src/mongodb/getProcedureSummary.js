db.getCollection("models").aggregate(

	// Pipeline
	[
		// Stage 1
		{
			$match: {
			  // {procedureId}
			  "＄domain": "Model.FPMN.Procedure"
			}
		},

		// Stage 2
		{
			$project: {
			    "procedure": "$$ROOT"
			}
		},

		// Stage 3
		{
			$lookup: {
			    from: "models",
			    localField: "procedure.designId",
			    foreignField: "_id",
			    as: "diagram"
			}
		},

		// Stage 4
		{
			$unwind: "$diagram"
		},

		// Stage 5
		{
			$graphLookup: {
			    "from": "models",
			    "startWith": "$procedure._id",
			    "connectFromField": "_id",
			    "connectToField": "parentId",
			    "as": "phases",
			    "maxDepth": 0,
			    "restrictSearchWithMatch": {
			        "nextPhaseId": null,
			        "＄domain": "Model.FPMN.Phase"
			    }
			}
		},

		// Stage 6
		{
			$graphLookup: {
			      "from": "models",
			      "startWith": "$phases._id",
			      "connectFromField": "prevPhaseId",
			      "connectToField": "_id",
			      "as": "phases"
			}
		},

		// Stage 7
		{
			$unwind: "$phases"
		},

		// Stage 8
		{
			$graphLookup: {
			      "from": "models",
			      "startWith": "$phases._id",
			      "connectFromField": "_id",
			      "connectToField": "parentId",
			      "as": "eServices",
			      "restrictSearchWithMatch": {
			        "＄domain": "Model.FPMN.Interaction.Task",
                    "channels": "Model.FPMN.Interaction.Channel.EForm",
			        "eServiceId": {
			          "$exists": true
			        }
			      }
			}
		},

		// Stage 9
		{
			$addFields: {
			    "phases.eServiceIds": "$eServices.eServiceId"
			}
		},

		// Stage 10
		{
			$group: {
			      "_id": "$_id",
			      "procedure": {
			        "$first": "$procedure"
			      },
			      "diagram": {
			        "$first": "$diagram"
			      },
			      "phases": {
			        "$push": "$phases"
			      }
			}
		},

		// Stage 11
		{
			$match: {
			    "phases.eServiceIds": "2" // {eServiceId}
			}
		},

		// Stage 12
		{
			$addFields: {
			    "phases": {
			        "$map": {
			          "input": "$phases",
			          "as": "phase",
			          "in": {
			            "_id": "$$phase._id",
			            "eServiceIds": "$$phase.eServiceIds",
			            "language": "$$phase.language",
			            "name": "$$phase.name",
			            "documentation": "$$phase.documentation",
			            "url": {
			              "$concat": [
			                "{appDiagramUrl}",
			                "$diagram._id",
			                "/",
			                "$$phase._id"
			              ]
			            }
			          }
			        }
			      }
			}
		},

		// Stage 13
		{
			$project: {
			      "_id": "$procedure._id",
			      "notation": "$diagram.notation",
			      "version": "$diagram.version",
			      "created": "$diagram.created",
			      "lastModified": "$diagram.lastModified",
			      "language": "$procedure.language",
			      "name": "$procedure.name",
			      "documentation": "$procedure.documentation",
			      "mission": "$procedure.mission",
			      "regulatoryRefs": "$procedure.regulatoryRefs",
			      "phases": "$phases",
			      "url": {
			        "$concat": [
			          "{appDiagramUrl}",
			          "$diagram._id",
			          "/",
			          "$procedure._id"
			        ]
			      },
			      "svg": {
			        "$concat": [
			          "{appDiagramSvg}",
			          "$diagram._id",
			          ".svg"
			        ]
			      }
			}
		},

	]

	// Created with Studio 3T, the IDE for MongoDB - https://studio3t.com/

);

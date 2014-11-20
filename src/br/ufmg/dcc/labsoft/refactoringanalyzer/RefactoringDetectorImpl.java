package br.ufmg.dcc.labsoft.refactoringanalyzer;

import gr.uom.java.xmi.ASTReader2;
import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.diff.Refactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

public class RefactoringDetectorImpl implements RefactoringDetector {

	@Override
	public void detectAll(String projectFolder, RefactoringHandler handler) {
		long numberOfOkRevisions = 0;
		long numberOfMergeRevisions = 0;
		RevCommit currentCommit = null;
		RevCommit parentCommit = null;
		UMLModel currentUMLModel = null;
		UMLModel parentUMLModel = null;
		List<Refactoring> refactorings = new ArrayList<Refactoring>();
		Calendar startTime = Calendar.getInstance();

		try {
			RepositoryBuilder builder = new RepositoryBuilder();
			org.eclipse.jgit.lib.Repository repository = builder
					.setGitDir(new File(projectFolder + File.separator + ".git"))
					.readEnvironment() // scan environment GIT_* variables
					.findGitDir() // scan up the file system tree
					.build();

			// Inicializa repositorio
			Git git = new Git(repository);
			checkoutHead(git);
			currentUMLModel = new ASTReader2(new File(projectFolder)).getUmlModel();
			handler.handleCurrent(currentUMLModel);
			
			Ref head = repository.getRef(Constants.MASTER);
			String headId = head.getObjectId().getName();
			
			RevWalk walk = new RevWalk(repository);
			Iterable<RevCommit> logs = git.log().call();
			Iterator<RevCommit> i = logs.iterator();		

			//Itera em todas as revisoes do projeto
			while (i.hasNext()) {				
				currentCommit = walk.parseCommit(i.next());		

				if (currentCommit.getParentCount() == 1) {

					try {
						// Ganho de performance - Aproveita a UML Model que ja se encontra em memorioa da comparacao anterior
						if (parentCommit != null && currentCommit.getId().equals(parentCommit.getId())) {
							currentUMLModel = parentUMLModel;
						} else {
							// Faz checkout e gera UML model da revisao current
							checkoutCommand(git, currentCommit);
							currentUMLModel = null;
							currentUMLModel = new ASTReader2(new File(projectFolder)).getUmlModel();
						}
						
						// Recupera o parent commit
						parentCommit = walk.parseCommit(currentCommit.getParent(0));
						
						Revision prevRevision = new Revision(parentCommit, headId == parentCommit.getId().getName());
						Revision curRevision = new Revision(currentCommit, headId == currentCommit.getId().getName());
						//System.out.println(String.format("Comparando %s e %s", prevRevision.getId(), curRevision.getId()));
						
						// Faz checkout e gera UML model da revisao parent
						checkoutCommand(git, parentCommit);
						parentUMLModel = null;
						parentUMLModel = new ASTReader2(new File(projectFolder)).getUmlModel();
						
						// Diff entre currentModel e parentModel
						UMLModelDiff modelDiff = parentUMLModel.diff(currentUMLModel);
						List<Refactoring> refactoringsAtRevision = modelDiff.getRefactorings();
						refactorings.addAll(refactoringsAtRevision);
						handler.handleDiff(prevRevision, parentUMLModel, curRevision, currentUMLModel, refactoringsAtRevision);
						
						for (Refactoring ref : refactoringsAtRevision) {
							handler.handleRefactoring(curRevision, currentUMLModel, ref);
						}
					} catch (Exception e) {
						System.out.println("ERRO, revis�o ignorada: " + currentCommit.getId().getName() + "\n");
						e.printStackTrace();
					}

					numberOfOkRevisions++;
				} else {
					numberOfMergeRevisions++;
				}

				//System.out.println(String.format("Revis�es: %5d Ignoradas: %5d Refactorings: %4d ", numberOfOkRevisions, numberOfMergeRevisions, refactorings.size()));
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Calendar endTime = Calendar.getInstance();

		System.out.println("=====================================================");
		System.out.println(projectFolder);
		System.out.println(String.format("Revis�es: %5d  Merge: %5d  Refactorings: %4d ", numberOfOkRevisions, numberOfMergeRevisions, refactorings.size()));
		System.out.println("In�cio: " + startTime.get(Calendar.HOUR) + ":" + startTime.get(Calendar.MINUTE));
		System.out.println("Fim:    " + endTime.get(Calendar.HOUR) + ":" + endTime.get(Calendar.MINUTE));	
	}

	private void checkoutCommand(Git git, RevCommit commit) throws Exception {
		CheckoutCommand checkout = git.checkout().setStartPoint(commit).setName(commit.getId().getName());
		checkout.call();		
	}

	private void checkoutHead(Git git) throws Exception {
		CheckoutCommand checkout = git.checkout().setStartPoint(Constants.HEAD).setName(Constants.MASTER);
		checkout.call();
	}

}

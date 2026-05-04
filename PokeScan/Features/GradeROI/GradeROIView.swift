import SwiftUI

struct GradeROIView: View {
    let card: Card
    @StateObject private var viewModel = GradeROIViewModel()
    @ObservedObject private var store = StoreKitService.shared

    var body: some View {
        Group {
            if !store.isPro {
                PaywallView(onDismiss: {})
            } else {
                roiContent
            }
        }
        .navigationTitle("Grade ROI")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var roiContent: some View {
        List {
            Section {
                Picker("Condition", selection: $viewModel.condition) {
                    ForEach(CardCondition.allCases, id: \.self) { c in
                        Text(c.shortLabel).tag(c)
                    }
                }
                .pickerStyle(.segmented)
                Text(viewModel.condition.label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } header: {
                Text("Card Condition")
            }

            Section("Grading Service") {
                Picker("Service", selection: $viewModel.gradingService) {
                    ForEach(GradingService.allCases, id: \.self) { s in
                        Text(s.label).tag(s)
                    }
                }
                .pickerStyle(.segmented)
                Text("Fee: \(viewModel.gradingService.fee, format: .currency(code: "USD"))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Section {
                Button {
                    Task { await viewModel.calculate(card: card) }
                } label: {
                    if viewModel.isLoading {
                        ProgressView().frame(maxWidth: .infinity)
                    } else {
                        Text("Calculate ROI").frame(maxWidth: .infinity)
                    }
                }
                .disabled(viewModel.isLoading || card.marketPrice == nil)
            }

            if let result = viewModel.result {
                Section("Results") {
                    LabeledContent("Raw Price") {
                        Text(card.marketPrice ?? 0, format: .currency(code: "USD"))
                    }
                    LabeledContent("Expected Grade", value: "\(viewModel.gradingService.label) \(result.expectedGrade)")
                    LabeledContent("Graded Value") {
                        Text(result.gradedMarketValue, format: .currency(code: "USD"))
                    }
                    LabeledContent("Grading Fee") {
                        Text(result.gradingFee, format: .currency(code: "USD"))
                    }
                    LabeledContent("Net ROI") {
                        Text(result.netROI, format: .currency(code: "USD"))
                            .foregroundStyle(result.netROI >= 0 ? .green : .red)
                    }
                    if let beg = result.breakEvenGrade {
                        LabeledContent("Break-even Grade", value: "\(viewModel.gradingService.label) \(beg)+")
                    } else {
                        LabeledContent("Break-even Grade", value: "Not profitable")
                    }
                    LabeledContent("Confidence", value: result.confidence.capitalized)
                }
            }

            if let error = viewModel.errorMessage {
                Section {
                    Text(error).foregroundStyle(.red).font(.caption)
                }
            }
        }
    }
}
